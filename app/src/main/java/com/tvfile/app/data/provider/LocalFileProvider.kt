package com.tvfile.app.data.provider

import android.os.Environment
import com.tvfile.app.data.model.StorageType
import com.tvfile.app.data.model.TVFile
import com.tvfile.app.util.MimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class LocalFileProvider : IStorageProvider {

    val rootPath: String
        get() = Environment.getExternalStorageDirectory().absolutePath

    override suspend fun listFiles(path: String): Result<List<TVFile>> = withContext(Dispatchers.IO) {
        runCatching {
            val targetDir = if (path.isBlank()) File(rootPath) else File(path)
            if (!targetDir.exists() || !targetDir.isDirectory) {
                return@runCatching emptyList()
            }

            val files = targetDir.listFiles() ?: return@runCatching emptyList()
            files.map { f ->
                val isDir = f.isDirectory
                val itemCount = if (isDir) f.list()?.size ?: 0 else 0
                TVFile(
                    id = f.absolutePath,
                    name = f.name,
                    path = f.absolutePath,
                    size = if (isDir) 0L else f.length(),
                    lastModified = f.lastModified(),
                    isDirectory = isDir,
                    mimeType = MimeUtils.getMimeType(f.name),
                    category = MimeUtils.getCategory(f.name, isDir),
                    storageType = StorageType.INTERNAL,
                    itemCount = itemCount
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        }
    }

    override suspend fun openInputStream(file: TVFile): Result<InputStream> = withContext(Dispatchers.IO) {
        runCatching {
            FileInputStream(File(file.path))
        }
    }

    override suspend fun openOutputStream(parentPath: String, fileName: String): Result<OutputStream> = withContext(Dispatchers.IO) {
        runCatching {
            val destFile = File(parentPath, fileName)
            destFile.parentFile?.mkdirs()
            FileOutputStream(destFile)
        }
    }

    override suspend fun createDirectory(parentPath: String, dirName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(parentPath, dirName)
            if (!dir.exists()) dir.mkdirs() else true
        }
    }

    override suspend fun delete(file: TVFile): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(file.path)
            if (target.isDirectory) target.deleteRecursively() else target.delete()
        }
    }

    override suspend fun rename(file: TVFile, newName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val src = File(file.path)
            val dest = File(src.parentFile, newName)
            src.renameTo(dest)
        }
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).exists()
    }

    override suspend fun getSpaceInfo(): Pair<Long, Long> = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        Pair(root.freeSpace, root.totalSpace)
    }
}
