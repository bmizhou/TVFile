package com.tvfile.app.util

import com.tvfile.app.data.model.FileCategory
import java.util.Locale

object MimeUtils {

    private val VIDEO_EXTS = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "ts", "m2ts", "iso", "rmvb", "vob"
    )

    private val AUDIO_EXTS = setOf(
        "mp3", "flac", "wav", "aac", "ogg", "m4a", "wma", "ape", "dts", "ac3"
    )

    private val IMAGE_EXTS = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg", "raw", "heic"
    )

    private val APK_EXTS = setOf(
        "apk", "xapk", "apks"
    )

    private val DOC_EXTS = setOf(
        "txt", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "md", "epub"
    )

    fun getCategory(fileName: String, isDirectory: Boolean): FileCategory {
        if (isDirectory) return FileCategory.ALL
        val ext = getExtension(fileName).lowercase(Locale.ROOT)
        return when {
            VIDEO_EXTS.contains(ext) -> FileCategory.VIDEO
            AUDIO_EXTS.contains(ext) -> FileCategory.AUDIO
            IMAGE_EXTS.contains(ext) -> FileCategory.IMAGE
            APK_EXTS.contains(ext) -> FileCategory.APK
            DOC_EXTS.contains(ext) -> FileCategory.DOCUMENT
            else -> FileCategory.ALL
        }
    }

    fun getMimeType(fileName: String): String {
        val ext = getExtension(fileName).lowercase(Locale.ROOT)
        return when (ext) {
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "mp3" -> "audio/mpeg"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "apk" -> "application/vnd.android.package-archive"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            else -> "*/*"
        }
    }

    fun getExtension(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0 && dotIndex < fileName.length - 1) {
            fileName.substring(dotIndex + 1)
        } else {
            ""
        }
    }
}
