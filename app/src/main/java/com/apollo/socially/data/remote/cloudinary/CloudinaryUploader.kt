package com.apollo.socially.data.remote.cloudinary

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL


object CloudinaryUploader {

    private const val BOUNDARY = "SociallyCDNBoundary"
    private const val LINE_END = "\r\n"
    private const val TWO_HYPHENS = "--"

    // ── Public API ──────────────────────────────────────────────

    suspend fun uploadImage(
        context: Context,
        uri: Uri,
        folder: String = "socially",
        onProgress: ((Int) -> Unit)? = null
    ): UploadResult = upload(
        context, uri, CloudinaryConfig.IMAGE_UPLOAD_URL, "image", folder, onProgress
    )

    suspend fun uploadVideo(
        context: Context,
        uri: Uri,
        folder: String = "socially/videos",
        onProgress: ((Int) -> Unit)? = null
    ): UploadResult = upload(
        context, uri, CloudinaryConfig.VIDEO_UPLOAD_URL, "video", folder, onProgress
    )

    // ── Core upload ─────────────────────────────────────────────

    private suspend fun upload(
        context: Context,
        uri: Uri,
        uploadUrl: String,
        resourceType: String,
        folder: String,
        onProgress: ((Int) -> Unit)?
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext UploadResult.Error(Exception("Cannot open URI: $uri"))

            val bytes = inputStream.readBytes()
            inputStream.close()

            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val fileName = getFileName(context, uri)

            val connection = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
                doInput = true
                doOutput = true
                useCaches = false
                requestMethod = "POST"
                setRequestProperty("Connection", "Keep-Alive")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
            }

            val dos = DataOutputStream(connection.outputStream)

            // ── Part 1: upload_preset ──
            writeFormField(dos, "upload_preset", CloudinaryConfig.UPLOAD_PRESET)

            // ── Part 2: folder ──
            writeFormField(dos, "folder", folder)

            // ── Part 3: file bytes ──
            dos.writeBytes("$TWO_HYPHENS$BOUNDARY$LINE_END")
            dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"$LINE_END")
            dos.writeBytes("Content-Type: $mimeType$LINE_END")
            dos.writeBytes(LINE_END)

            val totalBytes = bytes.size
            var uploaded = 0
            val chunkSize = 4096
            var offset = 0
            while (offset < totalBytes) {
                val chunk = minOf(chunkSize, totalBytes - offset)
                dos.write(bytes, offset, chunk)
                offset += chunk
                uploaded += chunk
                onProgress?.invoke((uploaded * 100 / totalBytes))
            }

            dos.writeBytes(LINE_END)
            dos.writeBytes("$TWO_HYPHENS$BOUNDARY$TWO_HYPHENS$LINE_END")
            dos.flush()
            dos.close()

            val responseCode = connection.responseCode
            val responseStream = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = responseStream?.bufferedReader()?.readText() ?: ""
            responseStream?.close()
            connection.disconnect()

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UploadResult.Error(
                    Exception("Cloudinary error $responseCode: $responseText")
                )
            }

            val json = JSONObject(responseText)
            val secureUrl = json.getString("secure_url")
            val publicId  = json.getString("public_id")

            UploadResult.Success(
                url = secureUrl,
                publicId = publicId,
                resourceType = resourceType
            )

        } catch (e: Exception) {
            UploadResult.Error(e)
        }
    }

    private fun writeFormField(dos: DataOutputStream, name: String, value: String) {
        dos.writeBytes("$TWO_HYPHENS$BOUNDARY$LINE_END")
        dos.writeBytes("Content-Disposition: form-data; name=\"$name\"$LINE_END")
        dos.writeBytes(LINE_END)
        dos.writeBytes(value)
        dos.writeBytes(LINE_END)
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "upload_${System.currentTimeMillis()}"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx != -1 && cursor.moveToFirst()) {
                name = cursor.getString(idx) ?: name
            }
        }
        return name
    }
}
