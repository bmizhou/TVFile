package com.tvfile.app.data.provider

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.rapid7.client.dcerpc.mssrvs.ServerService
import com.rapid7.client.dcerpc.transport.SMBTransportFactories
import com.tvfile.app.data.model.StorageType
import com.tvfile.app.data.model.TVFile
import com.tvfile.app.util.MimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.EnumSet

class SmbStorageProvider(
    val serverId: Long,
    val host: String,
    val port: Int = 445,
    val username: String = "",
    val password: String = "",
    val isAnonymous: Boolean = false,
    val initialShare: String = ""
) : IStorageProvider {

    private val client = SMBClient()
    private var connection: Connection? = null
    private var session: Session? = null

    private fun getSession(): Session {
        val curSession = session
        if (curSession != null && curSession.connection.isConnected) {
            return curSession
        }
        val targetPort = if (port > 0) port else 445
        val conn = client.connect(host, targetPort)
        connection = conn
        val auth = if (isAnonymous || username.isBlank()) {
            AuthenticationContext.guest()
        } else {
            AuthenticationContext(username, password.toCharArray(), "")
        }
        val s = conn.authenticate(auth)
        session = s
        return s
    }

    private fun getDiskShare(shareName: String): DiskShare {
        return getSession().connectShare(shareName) as DiskShare
    }

    private fun parsePath(path: String): Pair<String, String> {
        val clean = path.trim('/', '\\').replace('\\', '/')
        val cleanInit = initialShare.trim('/', '\\').replace('\\', '/')

        if (clean.isBlank() || clean == "/") {
            return Pair(cleanInit, "")
        }
        val slashIndex = clean.indexOf('/')
        return if (slashIndex == -1) {
            Pair(clean, "")
        } else {
            Pair(clean.substring(0, slashIndex), clean.substring(slashIndex + 1))
        }
    }

    // 当路径为 / 且未指定具体共享名时，自动列出该 SMB 服务器上的所有磁盘共享文件夹
    private fun enumerateShares(): List<TVFile> {
        return try {
            val s = getSession()
            val transport = SMBTransportFactories.SRVSVC.getTransport(s)
            val service = ServerService(transport)
            service.shares1
                .filter { it.type == 0 && !it.netName.endsWith("$") }
                .map { info ->
                    TVFile(
                        id = "smb://$host/${info.netName}",
                        name = info.netName,
                        path = info.netName,
                        size = 0L,
                        lastModified = System.currentTimeMillis(),
                        isDirectory = true,
                        mimeType = "*/*",
                        category = com.tvfile.app.data.model.FileCategory.ALL,
                        storageType = StorageType.SMB,
                        serverId = serverId,
                        itemCount = 0
                    )
                }
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun listFiles(path: String): Result<List<TVFile>> = withContext(Dispatchers.IO) {
        runCatching {
            val (shareName, subPath) = parsePath(path)
            if (shareName.isBlank()) {
                // 用户未指定具体共享名（如路径为 /），列出主机上所有共享文件夹供进入！
                val shares = enumerateShares()
                if (shares.isNotEmpty()) {
                    return@runCatching shares
                }
                return@runCatching emptyList()
            }

            val share = getDiskShare(shareName)
            val list = share.list(subPath)

            list.filter { it.fileName != "." && it.fileName != ".." }.map { item ->
                val isDir = item.fileAttributes and 0x10L != 0L
                val fullItemPath = if (subPath.isBlank()) "$shareName/${item.fileName}" else "$shareName/$subPath/${item.fileName}"
                TVFile(
                    id = "smb://$host/$fullItemPath",
                    name = item.fileName,
                    path = fullItemPath,
                    size = if (isDir) 0L else item.endOfFile,
                    lastModified = item.lastWriteTime.toEpochMillis(),
                    isDirectory = isDir,
                    mimeType = MimeUtils.getMimeType(item.fileName),
                    category = MimeUtils.getCategory(item.fileName, isDir),
                    storageType = StorageType.SMB,
                    serverId = serverId
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        }
    }

    override suspend fun openInputStream(file: TVFile): Result<InputStream> = withContext(Dispatchers.IO) {
        runCatching {
            val (shareName, subPath) = parsePath(file.path)
            val share = getDiskShare(shareName)
            val f = share.openFile(
                subPath,
                EnumSet.of(AccessMask.FILE_READ_DATA),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
            f.inputStream
        }
    }

    override suspend fun openOutputStream(parentPath: String, fileName: String): Result<OutputStream> = withContext(Dispatchers.IO) {
        runCatching {
            val (shareName, subPath) = parsePath(parentPath)
            val fullSub = if (subPath.isBlank()) fileName else "$subPath/$fileName"
            val share = getDiskShare(shareName)
            val f = share.openFile(
                fullSub,
                EnumSet.of(AccessMask.FILE_WRITE_DATA),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OVERWRITE_IF,
                null
            )
            f.outputStream
        }
    }

    override suspend fun createDirectory(parentPath: String, dirName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val (shareName, subPath) = parsePath(parentPath)
            val fullSub = if (subPath.isBlank()) dirName else "$subPath/$dirName"
            val share = getDiskShare(shareName)
            share.mkdir(fullSub)
            true
        }
    }

    override suspend fun delete(file: TVFile): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val (shareName, subPath) = parsePath(file.path)
            val share = getDiskShare(shareName)
            if (file.isDirectory) share.rmdir(subPath, true) else share.rm(subPath)
            true
        }
    }

    override suspend fun rename(file: TVFile, newName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val (shareName, subPath) = parsePath(file.path)
            val lastSlash = subPath.lastIndexOf('/')
            val parentSub = if (lastSlash == -1) "" else subPath.substring(0, lastSlash)
            val newSubPath = if (parentSub.isBlank()) newName else "$parentSub/$newName"
            val share = getDiskShare(shareName)
            val smbFile = share.openFile(
                subPath,
                EnumSet.of(AccessMask.DELETE),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
            smbFile.rename(newSubPath, true)
            true
        }
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val (shareName, subPath) = parsePath(path)
            if (shareName.isBlank()) return@withContext true
            val share = getDiskShare(shareName)
            share.fileExists(subPath) || share.folderExists(subPath)
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getSpaceInfo(): Pair<Long, Long> = withContext(Dispatchers.IO) {
        Pair(0L, 0L)
    }

    fun close() {
        try {
            session?.close()
            connection?.close()
        } catch (_: Exception) {}
    }
}
