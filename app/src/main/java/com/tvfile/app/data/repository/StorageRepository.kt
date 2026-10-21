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

    suspend fun updateServer(server: ServerEntity) = serverDao.updateServer(server)

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

}
