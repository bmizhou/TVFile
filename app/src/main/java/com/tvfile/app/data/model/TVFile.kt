package com.tvfile.app.data.model

import android.net.Uri

data class TVFile(
    val id: String,
    val name: String,
    val path: String,
    val uri: Uri? = null,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val isDirectory: Boolean = false,
    val mimeType: String = "*/*",
    val category: FileCategory = FileCategory.ALL,
    val storageType: StorageType = StorageType.INTERNAL,
    val serverId: Long? = null,
    val itemCount: Int = 0
) {
    val formattedSize: String
        get() {
            if (isDirectory) return "${itemCount} 项"
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            if (digitGroups >= units.size) digitGroups = units.size - 1
            return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
        }
}
