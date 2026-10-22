package com.tvfile.app.data.repository

import android.content.Context
import com.tvfile.app.data.database.AppDatabase
import com.tvfile.app.data.database.ServerEntity
import com.tvfile.app.data.model.StorageDevice
import com.tvfile.app.data.model.StorageType
import com.tvfile.app.data.provider.*
import com.tvfile.app.data.streamer.LocalStreamerServer
import com.tvfile.app.util.UsbMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class StorageRepository(private val context: Context) {

    val localProvider = LocalFileProvider()
    val usbMonitor = UsbMonitor(context)
    val streamerServer = LocalStreamerServer()

    private val db = AppDatabase.getDatabase(context)
    private val serverDao = db.serverDao()
    val playbackProgressDao = db.playbackProgressDao()

    private val activeUsbProviders = mutableMapOf<String, UsbFileProvider>()
    private val activeNetworkProviders = mutableMapOf<Long, IStorageProvider>()

    init {
        usbMonitor.start()
        try {
            streamerServer.start()
        } catch (_: Exception) {}
    }

    val usbDevices: StateFlow<List<StorageDevice>>
        get() = usbMonitor.usbDevices

    fun getAllServers(): Flow<List<ServerEntity>> = serverDao.getAllServers()

    suspend fun addServer(server: ServerEntity): Long = serverDao.insertServer(server)

    suspend fun updateServer(server: ServerEntity) {
        serverDao.updateServer(server)
        activeNetworkProviders.remove(server.id)
    }

    suspend fun deleteServer(server: ServerEntity) {
        serverDao.deleteServer(server)
        activeNetworkProviders.remove(server.id)
    }

    fun getUsbProvider(mountPath: String): UsbFileProvider {
        return activeUsbProviders.getOrPut(mountPath) {
            UsbFileProvider(mountPath)
        }
    }

    suspend fun getNetworkProvider(serverId: Long): IStorageProvider? {
        val cached = activeNetworkProviders[serverId]
        if (cached != null) return cached

        val entity = serverDao.getServerById(serverId) ?: return null
        val provider: IStorageProvider = when (entity.storageType) {
            StorageType.SMB -> SmbStorageProvider(
                serverId = entity.id,
                host = entity.host,
                port = entity.port,
                username = entity.username,
                password = entity.password,
                isAnonymous = entity.isAnonymous,
                initialShare = entity.sharePath
            )
            StorageType.WEBDAV -> WebDavStorageProvider(
                serverId = entity.id,
                host = entity.host,
                port = entity.port,
                username = entity.username,
                password = entity.password,
                rootPath = entity.sharePath
            )
            else -> return null
        }
        activeNetworkProviders[serverId] = provider
        return provider
    }

    suspend fun getPlaybackProgress(fileId: String): com.tvfile.app.data.database.PlaybackProgressEntity? {
        return playbackProgressDao.getProgress(fileId)
    }

    suspend fun savePlaybackProgress(fileId: String, positionMs: Long, durationMs: Long) {
        if (durationMs > 0 && positionMs >= (durationMs * 0.95f).toLong()) {
            playbackProgressDao.deleteProgress(fileId)
        } else if (positionMs > 5000L) {
            playbackProgressDao.saveProgress(
                com.tvfile.app.data.database.PlaybackProgressEntity(
                    fileId = fileId,
                    positionMs = positionMs,
                    durationMs = durationMs
                )
            )
        }
    }

    fun getAllProgressFlow(): kotlinx.coroutines.flow.Flow<List<com.tvfile.app.data.database.PlaybackProgressEntity>> {
        return playbackProgressDao.getAllProgressFlow()
    }


    private var webTransferServer: com.tvfile.app.data.streamer.WebTransferServer? = null
    var onWebFilesChangedListener: (() -> Unit)? = null

    val tvFileFolder: java.io.File
        get() {
            val root = android.os.Environment.getExternalStorageDirectory()
            val folder = java.io.File(root, "tvfile")
            if (!folder.exists()) folder.mkdirs()
            return folder
        }

    fun isWebTransferRunning(): Boolean {
        return webTransferServer?.isAlive ?: false
    }

    fun startWebTransferServer(onChanged: () -> Unit): Boolean {
        onWebFilesChangedListener = onChanged
        return try {
            if (webTransferServer == null || !webTransferServer!!.isAlive) {
                webTransferServer = com.tvfile.app.data.streamer.WebTransferServer(context, 45789) {
                    onWebFilesChangedListener?.invoke()
                }
                webTransferServer?.start()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun stopWebTransferServer() {
        try {
            webTransferServer?.stop()
            webTransferServer = null
        } catch (_: Exception) {}
    }

    fun getReceivedFiles(): List<com.tvfile.app.data.model.TVFile> {
        val files = tvFileFolder.listFiles() ?: emptyArray()
        return files.filter { it.isFile }.map { f ->
            com.tvfile.app.data.model.TVFile(
                id = f.absolutePath,
                name = f.name,
                path = f.absolutePath,
                size = f.length(),
                lastModified = f.lastModified(),
                isDirectory = false,
                mimeType = com.tvfile.app.util.MimeUtils.getMimeType(f.name),
                category = com.tvfile.app.util.MimeUtils.getCategory(f.name, false),
                storageType = com.tvfile.app.data.model.StorageType.INTERNAL
            )
        }.sortedByDescending { it.lastModified }
    }

    fun deleteReceivedFile(file: com.tvfile.app.data.model.TVFile): Boolean {
        val target = java.io.File(file.path)
        return if (target.exists()) target.delete() else false
    }

    fun clearAllReceivedFiles(): Boolean {
        val files = tvFileFolder.listFiles() ?: return true
        for (f in files) {
            if (f.isFile) f.delete()
        }
        return true
    }


    private val prefs = context.getSharedPreferences("tvfile_user_preferences", android.content.Context.MODE_PRIVATE)

    fun getSavedSortMode(): com.tvfile.app.data.model.SortMode {
        val saved = prefs.getString("user_sort_mode", com.tvfile.app.data.model.SortMode.TIME_DESC.name) ?: com.tvfile.app.data.model.SortMode.TIME_DESC.name
        return try {
            com.tvfile.app.data.model.SortMode.valueOf(saved)
        } catch (_: Exception) {
            com.tvfile.app.data.model.SortMode.TIME_DESC
        }
    }

    fun saveSortMode(mode: com.tvfile.app.data.model.SortMode) {
        prefs.edit().putString("user_sort_mode", mode.name).apply()
    }

}
