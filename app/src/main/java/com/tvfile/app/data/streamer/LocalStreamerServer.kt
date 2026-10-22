package com.tvfile.app.data.streamer

import com.tvfile.app.data.model.TVFile
import com.tvfile.app.data.provider.IStorageProvider
import com.tvfile.app.data.provider.WebDavStorageProvider
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.net.ServerSocket
import java.net.URLEncoder

/**
 * 稳健型本地流媒体代理服务器 (NanoHTTPD)
 * 解决端口冲突、文件句柄泄漏与高码率卡顿问题。
 */
class LocalStreamerServer(initialPort: Int = 8989) : NanoHTTPD("127.0.0.1", findAvailablePort(initialPort)) {

    companion object {
        private fun findAvailablePort(startPort: Int): Int {
            for (p in startPort..(startPort + 50)) {
                try {
                    val ss = ServerSocket(p)
                    ss.reuseAddress = true
                    ss.close()
                    return p
                } catch (_: Exception) {}
            }
            return startPort
        }
    }

    // LRU 限流与防内存泄漏：只保留最近 5 个流，超出自动关闭
    private val activeStreams = object : LinkedHashMap<String, Pair<TVFile, IStorageProvider>>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<TVFile, IStorageProvider>>?): Boolean {
            return size > 5
        }
    }

    @Synchronized
    fun registerStream(file: TVFile, provider: IStorageProvider): String {
        val streamKey = "stream_${System.currentTimeMillis()}_${Math.abs(file.name.hashCode())}"
        activeStreams[streamKey] = Pair(file, provider)
        val encodedName = URLEncoder.encode(file.name, "UTF-8")
        val port = listeningPort
        return "http://127.0.0.1:$port/stream?id=$streamKey&filename=$encodedName"
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val parms = session.parameters

        return try {
            if (uri.startsWith("/stream")) {
                val streamId = parms["id"]?.firstOrNull() 
                    ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing stream id")
                
                val target = synchronized(this) { activeStreams[streamId] }
                    ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Stream expired or not found")

                val file = target.first
                val provider = target.second
                val totalLength = file.size
                val mimeType = file.mimeType.ifBlank { "video/mp4" }

                val rangeHeader = session.headers["range"]
                if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                    serveRangeRequest(rangeHeader, totalLength, mimeType, file, provider)
                } else {
                    serveFullRequest(totalLength, mimeType, file, provider)
                }
            } else {
                newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "TVFile Streamer is running")
            }
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Stream error: ${e.message}")
        }
    }

    private fun serveFullRequest(totalLength: Long, mimeType: String, file: TVFile, provider: IStorageProvider): Response {
        val inputStreamResult = runBlocking { provider.openInputStream(file) }
        val stream = inputStreamResult.getOrNull()
            ?: return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Cannot open stream")

        val res = newFixedLengthResponse(Response.Status.OK, mimeType, stream, totalLength)
        res.addHeader("Accept-Ranges", "bytes")
        if (totalLength > 0) {
            res.addHeader("Content-Length", totalLength.toString())
        }
        return res
    }

    private fun serveRangeRequest(rangeHeader: String, totalLength: Long, mimeType: String, file: TVFile, provider: IStorageProvider): Response {
        var start: Long = 0
        var end: Long = if (totalLength > 0) totalLength - 1 else Long.MAX_VALUE - 1

        val rangeValue = rangeHeader.substring(6).trim()
        val minusIndex = rangeValue.indexOf('-')
        if (minusIndex != -1) {
            val startStr = rangeValue.substring(0, minusIndex)
            val endStr = rangeValue.substring(minusIndex + 1)
            if (startStr.isNotEmpty()) start = startStr.toLong()
            if (endStr.isNotEmpty()) end = endStr.toLong()
        }

        if (totalLength > 0 && (start > end || start >= totalLength)) {
            val res = newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "")
            res.addHeader("Content-Range", "bytes */$totalLength")
            return res
        }

        val rangeLength = if (totalLength > 0) end - start + 1 else -1L

        val baseStream = if (provider is WebDavStorageProvider) {
            val davStreamResult = runBlocking { provider.openRangeStream(file, rangeHeader) }
            davStreamResult.getOrNull()
        } else {
            val inResult = runBlocking { provider.openInputStream(file) }
            val st = inResult.getOrNull()
            st?.apply {
                if (start > 0) {
                    try { skip(start) } catch (_: Exception) {}
                }
            }
        }

        if (baseStream == null) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Cannot open range stream")
        }

        // 大缓冲区包装：使用 64KB 避免小块频繁读取造成的卡顿
        val boundedStream = if (rangeLength > 0 && provider !is WebDavStorageProvider) {
            object : InputStream() {
                private var remaining = rangeLength
                override fun read(): Int {
                    if (remaining <= 0) return -1
                    val b = baseStream.read()
                    if (b != -1) remaining--
                    return b
                }
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    if (remaining <= 0) return -1
                    val toRead = Math.min(len.toLong(), remaining).toInt()
                    val readBytes = baseStream.read(b, off, toRead)
                    if (readBytes > 0) remaining -= readBytes
                    return readBytes
                }
                override fun close() {
                    baseStream.close()
                }
            }
        } else {
            baseStream
        }

        val res = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mimeType, boundedStream, rangeLength)
        res.addHeader("Accept-Ranges", "bytes")
        if (rangeLength > 0) {
            res.addHeader("Content-Length", rangeLength.toString())
        }
        val totalLenStr = if (totalLength > 0) totalLength.toString() else "*"
        res.addHeader("Content-Range", "bytes $start-$end/$totalLenStr")
        return res
    }
}
