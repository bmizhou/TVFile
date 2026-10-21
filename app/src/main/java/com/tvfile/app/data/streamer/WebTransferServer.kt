package com.tvfile.app.data.streamer

import android.content.Context
import android.os.Environment
import com.tvfile.app.util.MimeUtils
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.net.URLDecoder

class WebTransferServer(
    private val context: Context,
    port: Int = 45789,
    private val onFilesChanged: () -> Unit = {}
) : NanoHTTPD("0.0.0.0", port) {

    val targetFolder: File
        get() {
            val root = Environment.getExternalStorageDirectory()
            val folder = File(root, "tvfile")
            if (!folder.exists()) {
                folder.mkdirs()
            }
            return folder
        }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        return try {
            when {
                uri == "/" && method == Method.GET -> {
                    serveHtmlPage()
                }
                uri == "/api/files" && method == Method.GET -> {
                    serveFileListJson()
                }
                uri == "/api/upload" && method == Method.POST -> {
                    handleFileUpload(session)
                }
                (uri == "/api/delete" || uri == "/api/files") && (method == Method.POST || method == Method.DELETE) -> {
                    handleDeleteFile(session)
                }
                uri.startsWith("/api/download") && method == Method.GET -> {
                    handleDownloadFile(session)
                }
                else -> {
                    newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found")
                }
            }
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: " + e.message)
        }
    }

    private fun serveFileListJson(): Response {
        val files = targetFolder.listFiles() ?: emptyArray()
        val jsonSb = StringBuilder("[")
        var first = true

        val sortedFiles = files.filter { it.isFile }.sortedByDescending { it.lastModified() }
        for (f in sortedFiles) {
            if (!first) jsonSb.append(",")
            first = false

            val type = when (MimeUtils.getCategory(f.name, false).name) {
                "APK" -> "apk"
                "VIDEO" -> "video"
                "AUDIO" -> "audio"
                "IMAGE" -> "image"
                else -> "doc"
            }

            val safeName = f.name.replace("\"", "\\\"")
            jsonSb.append("{\"name\":\"").append(safeName).append("\",")
            jsonSb.append("\"size\":").append(f.length()).append(",")
            jsonSb.append("\"time\":").append(f.lastModified()).append(",")
            jsonSb.append("\"type\":\"").append(type).append("\"}")
        }
        jsonSb.append("]")

        val res = newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", jsonSb.toString())
        res.addHeader("Access-Control-Allow-Origin", "*")
        return res
    }

    private fun handleFileUpload(session: IHTTPSession): Response {
        val files = HashMap<String, String>()
        session.parseBody(files)

        val parms = session.parameters
        var uploadedCount = 0

        for ((key, tempPath) in files) {
            val originalName = parms[key]?.firstOrNull() ?: key
            val cleanName = File(originalName).name.ifBlank { "uploaded_file_" + System.currentTimeMillis() }

            val tempFile = File(tempPath)
            val destFile = File(targetFolder, cleanName)

            if (tempFile.exists()) {
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
                uploadedCount++
            }
        }

        onFilesChanged()
        val res = newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":true,\"count\":" + uploadedCount + "}")
        res.addHeader("Access-Control-Allow-Origin", "*")
        return res
    }

    private fun handleDeleteFile(session: IHTTPSession): Response {
        val nameParam = session.parameters["name"]?.firstOrNull()
        if (nameParam.isNullOrBlank()) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Missing file name\"}")
        }

        val cleanName = URLDecoder.decode(nameParam, "UTF-8")
        val target = File(targetFolder, cleanName)
        val deleted = if (target.exists()) target.delete() else false

        if (deleted) {
            onFilesChanged()
        }

        val res = newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":" + deleted + "}")
        res.addHeader("Access-Control-Allow-Origin", "*")
        return res
    }

    private fun handleDownloadFile(session: IHTTPSession): Response {
        val nameParam = session.parameters["name"]?.firstOrNull()
            ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing name")
        val cleanName = URLDecoder.decode(nameParam, "UTF-8")
        val file = File(targetFolder, cleanName)

        if (!file.exists() || !file.isFile) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found")
        }

        val inStream = FileInputStream(file)
        val mime = MimeUtils.getMimeType(file.name)
        val res = newFixedLengthResponse(Response.Status.OK, mime, inStream, file.length())
        res.addHeader("Content-Disposition", "attachment; filename=\"" + file.name + "\"")
        return res
    }

    private fun serveHtmlPage(): Response {
        val html = try {
            context.assets.open("web_transfer.html").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "<html><body><h2>TVFile Web Transfer</h2><p>Cannot load page: " + e.message + "</p></body></html>"
        }
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html)
    }
}