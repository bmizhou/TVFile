package com.tvfile.app.data.provider

import com.tvfile.app.data.model.StorageType
import com.tvfile.app.data.model.TVFile
import com.tvfile.app.util.MimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.w3c.dom.Element
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

class WebDavStorageProvider(
    val serverId: Long,
    val host: String,
    val port: Int = 5005,
    val username: String = "",
    val password: String = "",
    val rootPath: String = ""
) : IStorageProvider {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .authenticator { _, response ->
            if (response.request.header("Authorization") != null) return@authenticator null
            val credential = Credentials.basic(username, password)
            response.request.newBuilder().header("Authorization", credential).build()
        }
        .build()

        val baseUrl: String
        get() {
            val protocol = if (port == 443) "https" else "http"
            val portStr = if ((protocol == "http" && port == 80) || (protocol == "https" && port == 443)) "" else ":$port"
            val cleanRoot = rootPath.trim('/', '\\')
            val rootSegment = if (cleanRoot.isBlank()) "" else "/$cleanRoot"
            return "$protocol://$host$portStr$rootSegment"
        }

    fun buildUrl(path: String): String {
        return getFullUrl(path)
    }

    fun getFullUrl(path: String): String {
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val clean = path.trim('/', '\\')
        if (clean.isBlank()) return "$baseUrl/"
        
        val cleanRoot = rootPath.trim('/', '\\')
        if (cleanRoot.isNotBlank() && clean.startsWith(cleanRoot)) {
            val protocol = if (port == 443) "https" else "http"
            val portStr = if ((protocol == "http" && port == 80) || (protocol == "https" && port == 443)) "" else ":$port"
            return "$protocol://$host$portStr/$clean"
        }
        return "$baseUrl/$clean"
    }

    suspend fun openRangeStream(file: TVFile, rangeHeader: String): Result<InputStream> = withContext(Dispatchers.IO) {
        runCatching {
            val url = getFullUrl(file.path)
            val request = Request.Builder()
                .url(url)
                .header("Range", rangeHeader)
                .get()
                .build()
            val response = client.newCall(request).execute()
            response.body?.byteStream() ?: throw IllegalStateException("Empty body")
        }
    }

    private fun oldBuildUrl(path: String): String {
        val clean = path.trim('/')
        return if (clean.isBlank()) baseUrl + "/" else "$baseUrl/$clean"
    }

    override suspend fun listFiles(path: String): Result<List<TVFile>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = buildUrl(path)
            val propfindXml = """<?xml version="1.0" encoding="utf-8" ?>
                <D:propfind xmlns:D="DAV:">
                    <D:prop>
                        <D:displayname/>
                        <D:resourcetype/>
                        <D:getcontentlength/>
                        <D:getlastmodified/>
                    </D:prop>
                </D:propfind>
            """.trimIndent()

            val request = Request.Builder()
                .url(url)
                .method("PROPFIND", propfindXml.toRequestBody("text/xml".toMediaTypeOrNull()))
                .header("Depth", "1")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@runCatching emptyList()
            }

            val xmlStream = response.body?.byteStream() ?: return@runCatching emptyList()
            val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
                .newDocumentBuilder().parse(xmlStream)

            val responses = doc.getElementsByTagNameNS("*", "response")
            val list = mutableListOf<TVFile>()

            for (i in 0 until responses.length) {
                val respEl = responses.item(i) as Element
                val href = respEl.getElementsByTagNameNS("*", "href").item(0)?.textContent ?: ""
                
                // 跳过自身目录
                val cleanHref = href.trim('/')
                val cleanUrl = url.substringAfter("://").substringAfter('/').trim('/')
                if (cleanHref == cleanUrl || cleanHref.isBlank()) continue

                val isDir = respEl.getElementsByTagNameNS("*", "collection").length > 0
                val rawDisplayName = respEl.getElementsByTagNameNS("*", "displayname").item(0)?.textContent
                    ?: href.trimEnd('/').substringAfterLast('/')
                // 解决中文标题乱码/URL编码显示错误
                val displayName = try {
                    java.net.URLDecoder.decode(rawDisplayName, "UTF-8")
                } catch (_: Exception) {
                    rawDisplayName
                }

                val contentLength = respEl.getElementsByTagNameNS("*", "getcontentlength").item(0)?.textContent?.toLongOrNull() ?: 0L
                val lastModStr = respEl.getElementsByTagNameNS("*", "getlastmodified").item(0)?.textContent ?: ""
                val lastModifiedTime = parseWebDavDate(lastModStr)

                list.add(
                    TVFile(
                        id = href,
                        name = displayName,
                        path = href,
                        size = if (isDir) 0L else contentLength,
                        lastModified = lastModifiedTime,
                        isDirectory = isDir,
                        mimeType = MimeUtils.getMimeType(displayName),
                        category = MimeUtils.getCategory(displayName, isDir),
                        storageType = StorageType.WEBDAV,
                        serverId = serverId
                    )
                )
            }

            list.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        }
    }

    override suspend fun openInputStream(file: TVFile): Result<InputStream> = withContext(Dispatchers.IO) {
        runCatching {
            val url = getFullUrl(file.path)
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            response.body?.byteStream() ?: throw IllegalStateException("Empty response body")
        }
    }

    override suspend fun openOutputStream(parentPath: String, fileName: String): Result<OutputStream> = withContext(Dispatchers.IO) {
        runCatching {
            val url = buildUrl("$parentPath/$fileName")
            val pipedOut = PipedOutputStream()
            val pipedIn = PipedInputStream(pipedOut)

            // 启动异步上传
            Thread {
                val reqBody = object : RequestBody() {
                    override fun contentType(): MediaType? = "application/octet-stream".toMediaTypeOrNull()
                    override fun writeTo(sink: okio.BufferedSink) {
                        pipedIn.use { input ->
                            val buf = ByteArray(8192)
                            var read: Int
                            while (input.read(buf).also { read = it } != -1) {
                                sink.write(buf, 0, read)
                            }
                        }
                    }
                }
                val request = Request.Builder().url(url).put(reqBody).build()
                client.newCall(request).execute()
            }.start()

            pipedOut
        }
    }

    override suspend fun createDirectory(parentPath: String, dirName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val url = buildUrl("$parentPath/$dirName")
            val request = Request.Builder().url(url).method("MKCOL", null).build()
            val response = client.newCall(request).execute()
            response.isSuccessful || response.code == 405
        }
    }

    override suspend fun delete(file: TVFile): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val url = if (file.path.startsWith("http")) file.path else buildUrl(file.path)
            val request = Request.Builder().url(url).delete().build()
            client.newCall(request).execute().isSuccessful
        }
    }

    override suspend fun rename(file: TVFile, newName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val srcUrl = if (file.path.startsWith("http")) file.path else buildUrl(file.path)
            val parent = file.path.trimEnd('/').substringBeforeLast('/', "")
            val destUrl = buildUrl(if (parent.isBlank()) newName else "$parent/$newName")

            val request = Request.Builder()
                .url(srcUrl)
                .method("MOVE", null)
                .header("Destination", destUrl)
                .header("Overwrite", "T")
                .build()

            client.newCall(request).execute().isSuccessful
        }
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(path)
            val request = Request.Builder().url(url).head().build()
            client.newCall(request).execute().isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getSpaceInfo(): Pair<Long, Long> = withContext(Dispatchers.IO) {
        Pair(0L, 0L)
    }

    private fun parseWebDavDate(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        return try {
            val format = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US)
            format.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

}
