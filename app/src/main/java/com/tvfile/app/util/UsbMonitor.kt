package com.tvfile.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import com.tvfile.app.data.model.StorageDevice
import com.tvfile.app.data.model.StorageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.lang.reflect.Method

class UsbMonitor(private val context: Context) {

    private val _usbDevices = MutableStateFlow<List<StorageDevice>>(emptyList())
    val usbDevices: StateFlow<List<StorageDevice>> = _usbDevices.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            scanUsbDevices()
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_REMOVED)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addDataScheme("file")
        }
        context.registerReceiver(receiver, filter)
        scanUsbDevices()
    }

    fun stop() {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {}
    }

    fun scanUsbDevices() {
        val list = mutableListOf<StorageDevice>()
        val sm = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val volumes = sm.storageVolumes
            for (volume in volumes) {
                if (volume.isRemovable) {
                    val uuid = volume.uuid ?: continue
                    val description = volume.getDescription(context) ?: "外接 U 盘"
                    val path = getVolumeDirectory(volume) ?: "/storage/$uuid"
                    val file = File(path)
                    val total = file.totalSpace
                    val free = file.freeSpace

                    list.add(
                        StorageDevice(
                            id = "usb_$uuid",
                            name = description,
                            path = path,
                            storageType = StorageType.USB,
                            totalSpace = total,
                            freeSpace = free,
                            isOnline = true,
                            fsType = "FAT/NTFS"
                        )
                    )
                }
            }
        }

        // 兜底扫描常见 TV 盒子外接挂载路径（例如 /storage/xxxx-xxxx）
        if (list.isEmpty()) {
            val storageDir = File("/storage")
            if (storageDir.exists() && storageDir.isDirectory) {
                val subFiles = storageDir.listFiles() ?: emptyArray()
                for (f in subFiles) {
                    if (f.name.contains("-") && f.canRead()) {
                        list.add(
                            StorageDevice(
                                id = "usb_${f.name}",
                                name = "U 盘 (${f.name})",
                                path = f.absolutePath,
                                storageType = StorageType.USB,
                                totalSpace = f.totalSpace,
                                freeSpace = f.freeSpace,
                                isOnline = true,
                                fsType = "External"
                            )
                        )
                    }
                }
            }
        }

        _usbDevices.value = list
    }

    private fun getVolumeDirectory(volume: Any): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val vol = volume as android.os.storage.StorageVolume
                vol.directory?.absolutePath
            } else {
                val getPathMethod: Method = volume.javaClass.getMethod("getPath")
                getPathMethod.invoke(volume) as? String
            }
        } catch (e: Exception) {
            null
        }
    }
}
