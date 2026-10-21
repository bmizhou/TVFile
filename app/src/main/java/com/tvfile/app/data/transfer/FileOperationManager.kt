package com.tvfile.app.data.transfer

import com.tvfile.app.data.model.*
import com.tvfile.app.data.provider.IStorageProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * 全局剪贴板与跨存储传输引擎
 * 支持在 内部存储 / U盘 / SMB / WebDAV 任意两端进行高效管道流复制、剪切与粘贴。
 */
class FileOperationManager {

    data class ClipboardState(
        val files: List<TVFile> = emptyList(),
        val sourceProvider: IStorageProvider? = null,
        val operation: TransferOp = TransferOp.COPY
    )

    private val _clipboard = MutableStateFlow<ClipboardState?>(null)
    val clipboard: StateFlow<ClipboardState?> = _clipboard.asStateFlow()

    private val _activeTask = MutableStateFlow<TransferTask?>(null)
    val activeTask: StateFlow<TransferTask?> = _activeTask.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var transferJob: Job? = null

    fun copyToClipboard(files: List<TVFile>, provider: IStorageProvider) {
        _clipboard.value = ClipboardState(files, provider, TransferOp.COPY)
    }

    fun cutToClipboard(files: List<TVFile>, provider: IStorageProvider) {
        _clipboard.value = ClipboardState(files, provider, TransferOp.CUT)
    }

    fun clearClipboard() {
        _clipboard.value = null
    }

    fun startPaste(destParentPath: String, destProvider: IStorageProvider, destStorageType: StorageType) {
        val clip = _clipboard.value ?: return
        if (clip.files.isEmpty() || clip.sourceProvider == null) return

        transferJob?.cancel()
        transferJob = coroutineScope.launch {
            for (file in clip.files) {
                val task = TransferTask(
                    taskId = UUID.randomUUID().toString(),
                    sourceFile = file,
                    destParentPath = destParentPath,
                    destStorageType = destStorageType,
                    operation = clip.operation,
                    totalBytes = file.size,
                    transferredBytes = 0L,
                    state = TransferState.RUNNING
                )
                _activeTask.value = task

                val success = transferSingleFile(file, clip.sourceProvider, destParentPath, destProvider, task)
                if (success) {
                    if (clip.operation == TransferOp.CUT) {
                        clip.sourceProvider.delete(file)
                    }
                    task.state = TransferState.COMPLETED
                } else {
                    task.state = TransferState.FAILED
                }
                _activeTask.value = task.copy()
            }
            if (clip.operation == TransferOp.CUT) {
                clearClipboard()
            }
            delay(1500)
            _activeTask.value = null
        }
    }

    private suspend fun transferSingleFile(
        file: TVFile,
        srcProvider: IStorageProvider,
        destParentPath: String,
        destProvider: IStorageProvider,
        task: TransferTask
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val inStreamResult = srcProvider.openInputStream(file)
            val outStreamResult = destProvider.openOutputStream(destParentPath, file.name)

            val inStream = inStreamResult.getOrNull() ?: return@withContext false
            val outStream = outStreamResult.getOrNull() ?: return@withContext false

            inStream.use { input ->
                outStream.use { output ->
                    val buffer = ByteArray(2 * 1024 * 1024) // 2MB 高效分块
                    var bytesRead: Int
                    var lastTime = System.currentTimeMillis()
                    var bytesSinceLast = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (!isActive) {
                            task.state = TransferState.CANCELLED
                            return@withContext false
                        }
                        output.write(buffer, 0, bytesRead)
                        task.transferredBytes += bytesRead
                        bytesSinceLast += bytesRead

                        val now = System.currentTimeMillis()
                        val diff = now - lastTime
                        if (diff >= 500) {
                            task.speedBytesPerSec = (bytesSinceLast * 1000) / diff
                            lastTime = now
                            bytesSinceLast = 0L
                            _activeTask.value = task.copy()
                        }
                    }
                    output.flush()
                }
            }
            true
        } catch (e: Exception) {
            task.errorMessage = e.message
            false
        }
    }

    fun cancelTask() {
        transferJob?.cancel()
        _activeTask.value?.let {
            it.state = TransferState.CANCELLED
            _activeTask.value = it.copy()
        }
    }
}
