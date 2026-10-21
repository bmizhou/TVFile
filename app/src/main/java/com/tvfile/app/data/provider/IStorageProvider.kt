package com.tvfile.app.data.provider

import com.tvfile.app.data.model.TVFile
import java.io.InputStream
import java.io.OutputStream

interface IStorageProvider {
    suspend fun listFiles(path: String): Result<List<TVFile>>
    suspend fun openInputStream(file: TVFile): Result<InputStream>
    suspend fun openOutputStream(parentPath: String, fileName: String): Result<OutputStream>
    suspend fun createDirectory(parentPath: String, dirName: String): Result<Boolean>
    suspend fun delete(file: TVFile): Result<Boolean>
    suspend fun rename(file: TVFile, newName: String): Result<Boolean>
    suspend fun exists(path: String): Boolean
    suspend fun getSpaceInfo(): Pair<Long, Long> // Pair(freeSpace, totalSpace)
}
