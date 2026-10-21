package com.tvfile.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.tvfile.app.data.model.TVFile
import com.tvfile.app.data.provider.IStorageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ApkInstaller {

    fun installApk(context: Context, apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val hasInstallPermission = context.packageManager.canRequestPackageInstalls()
            if (!hasInstallPermission) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }

        val apkUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(installIntent)
    }

    suspend fun downloadAndInstall(
        context: Context,
        file: TVFile,
        provider: IStorageProvider,
        onProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val cacheFile = File(context.cacheDir, "temp_install_${file.name}")
        val inStreamResult = provider.openInputStream(file)
        val inStream = inStreamResult.getOrNull() ?: return@withContext

        inStream.use { input ->
            FileOutputStream(cacheFile).use { output ->
                val buf = ByteArray(1024 * 1024)
                var bytesRead: Int
                var total = 0L
                val fileLength = file.size
                while (input.read(buf).also { bytesRead = it } != -1) {
                    output.write(buf, 0, bytesRead)
                    total += bytesRead
                    if (fileLength > 0) {
                        onProgress(((total * 100) / fileLength).toInt())
                    }
                }
                output.flush()
            }
        }

        withContext(Dispatchers.Main) {
            installApk(context, cacheFile)
        }
    }
}
