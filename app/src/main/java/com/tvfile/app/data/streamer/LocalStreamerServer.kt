package com.tvfile.app.data.streamer

import com.tvfile.app.data.model.TVFile
import com.tvfile.app.data.provider.IStorageProvider
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.net.URLEncoder

/**
 * 轻量本地流媒体代理服务器 (NanoHTTPD)
 * 解决 ExoPlayer 无法直接 Seek 远程 SMB/WebDAV 流媒体文件的痛点，
 * 将远程流桥接为标准 HTTP 206 Partial Content 流，实现免下载即点即播和流畅拖动进度条。
 */
class LocalStreamerServer(port: Int = 8989) : NanoHTTPD("127.0.0.1", port) {

    private val activeStreams = mutableMapOf<String, Pair<TVFile, IStorageProvider>>()

    fun registerStream(file: TVFile, provider: IStorageProvider): String {
        val streamKey = "stream_${System.currentTimeMillis()}_${file.name.hashCode()}"
        activeStreams[streamKey] = Pair(file, provider)
        val encodedName = URLEncoder.encode(file.name, "UTF-8")
        return "http://127.0.0.1:$listeningPort/stream?id=$streamKey&filename=$encodedName"
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val parms = session.parameters

        if (uri.startsWith("/stream")) {
            val streamId = parms["id"]?.firstOrNull() ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing stream id")
            val target = activeStreams[streamId] ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Stream expired or not found")

            val file = target.first
            val provider = target.second
            val totalLength = file.size
            val mimeType = file.mimeType

            val rangeHeader = session.headers["range"]
            return if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                serveRangeRequest(rangeHeader, totalLength, mimeType, file, provider)
            } else {
                serveFullRequest(totalLength, mimeType, file, provider)
            }
        }

        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "TVFile Streamer is running")
    }

    private fun serveFullRequest(totalLength: Long, mimeType: String, file: TVFile, provider: IStorageProvider): Response {
        val inputStreamResult = runBlocking { provider.openInputStream(file) }
        val stream = inputStreamResult.getOrNull()
            ?: return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Cannot open stream")

        val res = newFixedLengthResponse(Response.Status.OK, mimeType, stream, totalLength)
        res.addHeader("Accept-Ranges", "bytes")
        res.addHeader("Content-Length", totalLength.toString())
        return res
    }

    private fun serveRangeRequest(rangeHeader: String, totalLength: Long, mimeType: String, file: TVFile, provider: IStorageProvider): Response {
        var start: Long = 0
        var end: Long = totalLength - 1

        val rangeValue = rangeHeader.substring(6).trim()
        val minusIndex = rangeValue.indexOf('-')
        if (minusIndex != -1) {
            val startStr = rangeValue.substring(0, minusIndex)
            val endStr = rangeValue.substring(minusIndex + 1)
            if (startStr.isNotEmpty()) start = startStr.toLong()
            if (endStr.isNotEmpty()) end = endStr.toLong()
        }

        if (start > end || start >= totalLength) {
            val res = newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "")
            res.addHeader("Content-Range", "bytes */$totalLength")
            return res
        }

        val rangeLength = end - start + 1
        val inputStreamResult = runBlocking { provider.openInputStream(file) }
        val baseStream = inputStreamResult.getOrNull()
            ?: return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Cannot open stream")

        // 跳过 start 字节
        baseStream.skip(start)

        val boundedStream = object : InputStream() {
            private var remaining = rangeLength
            override fun read(): Int {
                if (remaining <= 0) return -1
                val b = baseStream.read()
                if (b != -1) remaining--
                return b
            }
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (remaining <= 0) return -1
                val toRead = Math.min(len.toLong(), remaining).toInt()
                val readBytes = baseStream.read(b, off, toRead)
                if (readBytes > 0) remaining -= readBytes
                return readBytes
            }
            override fun close() {
                baseStream.close()
            }
        }

        val res = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mimeType, boundedStream, rangeLength)
        res.addHeader("Accept-Ranges", "bytes")
        res.addHeader("Content-Length", rangeLength.toString())
        res.addHeader("Content-Range", "bytes $start-$end/$totalLength")
        return res
    }
}
