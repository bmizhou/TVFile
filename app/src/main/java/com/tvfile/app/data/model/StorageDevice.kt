package com.tvfile.app.data.model

data class StorageDevice(
    val id: String,
    val name: String,
    val path: String,
    val storageType: StorageType,
    val totalSpace: Long = 0L,
    val freeSpace: Long = 0L,
    val isOnline: Boolean = true,
    val fsType: String = "",
    // 网络存储专属参数
    val host: String = "",
    val port: Int = 0,
    val username: String = "",
    val password: String = "",
    val sharePath: String = "",
    val serverId: Long? = null
) {
    val formattedCapacity: String
        get() {
            fun format(bytes: Long): String {
                if (bytes <= 0) return "0 GB"
                val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                return if (gb >= 1000) String.format("%.1f TB", gb / 1024.0) else String.format("%.1f GB", gb)
            }
            return if (totalSpace > 0) "${format(freeSpace)} 可用 / ${format(totalSpace)}" else "容量未知"
        }
}
