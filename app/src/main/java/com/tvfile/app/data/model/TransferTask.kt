package com.tvfile.app.data.model

enum class TransferOp {
    COPY,
    CUT
}

enum class TransferState {
    WAITING,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class TransferTask(
    val taskId: String,
    val sourceFile: TVFile,
    val destParentPath: String,
    val destStorageType: StorageType,
    val operation: TransferOp = TransferOp.COPY,
    var totalBytes: Long = 0L,
    var transferredBytes: Long = 0L,
    var speedBytesPerSec: Long = 0L,
    var state: TransferState = TransferState.WAITING,
    var errorMessage: String? = null
) {
    val progressPercent: Int
        get() = if (totalBytes > 0) ((transferredBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0

    val formattedSpeed: String
        get() {
            val mb = speedBytesPerSec.toDouble() / (1024.0 * 1024.0)
            return String.format("%.1f MB/s", mb)
        }
}
