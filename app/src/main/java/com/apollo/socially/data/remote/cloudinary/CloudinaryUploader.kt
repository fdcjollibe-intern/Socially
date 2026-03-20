package com.apollo.socially.data.remote.cloudinary

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object CloudinaryUploader {

    private const val TAG = "CloudinaryUploader"
    private const val BOUNDARY = "SociallyCDNBoundary"
    private const val LINE_END = "\r\n"
    private const val TWO_HYPHENS = "--"
    
    // Size limits
    const val MAX_FILE_SIZE = 100 * 1024 * 1024L // 100MB - Signed upload limit
    private const val WRITE_CHUNK_SIZE = 8192 // 8KB chunks for writing to output stream

    // ── Public API ──────────────────────────────────────────────

    suspend fun uploadImage(
        context: Context,
        uri: Uri,
        folder: String = "socially",
        onProgress: ((Int) -> Unit)? = null
    ): UploadResult = uploadWithSizeCheck(
        context, uri, CloudinaryConfig.IMAGE_UPLOAD_URL, 
        "image", folder, onProgress
    )

    suspend fun uploadVideo(
        context: Context,
        uri: Uri,
        folder: String = "socially/videos",
        onProgress: ((Int) -> Unit)? = null
    ): UploadResult = uploadWithSizeCheck(
        context, uri, CloudinaryConfig.VIDEO_UPLOAD_URL,
        "video", folder, onProgress
    )
    
    /**
     * Gets the file size for a given URI - made public for validation
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    return cursor.getLong(sizeIndex)
                }
            }
        }
        // Fallback: read the stream to get size
        context.contentResolver.openInputStream(uri)?.use {
            return it.available().toLong()
        }
        return 0L
    }
    
    /**
     * Formats file size in human-readable format
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        }
    }
    
    // ── Size Check & Router ─────────────────────────────────────
    
    private suspend fun uploadWithSizeCheck(
        context: Context,
        uri: Uri,
        uploadUrl: String,
        resourceType: String,
        folder: String,
        onProgress: ((Int) -> Unit)?
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            // Check file size first
            val fileSize = getFileSize(context, uri)
            
            Log.d(TAG, "Uploading $resourceType, size: ${formatFileSize(fileSize)}")
            
            if (fileSize <= 0) {
                return@withContext UploadResult.Error(
                    Exception("Cannot determine file size")
                )
            }
            
            if (fileSize > MAX_FILE_SIZE) {
                return@withContext UploadResult.Error(
                    Exception("File size exceeds 100MB limit. File is ${formatFileSize(fileSize)}")
                )
            }
            
            // Use streaming upload for all file sizes
            uploadStreaming(context, uri, uploadUrl, resourceType, folder, fileSize, onProgress)
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            UploadResult.Error(e)
        }
    }

    // ── Streaming upload (works for all file sizes) ─────────────────────────────────────────

    /**
     * Generate SHA-256 signature for signed uploads
     */
    private fun generateSignature(params: Map<String, String>): String {
        // Sort parameters alphabetically and create signature string
        val sortedParams = params.toSortedMap()
        val paramString = sortedParams.map { "${it.key}=${it.value}" }.joinToString("&")
        val stringToSign = "$paramString${CloudinaryConfig.API_SECRET}"
        
        // Generate SHA-1 hash
        val digest = MessageDigest.getInstance("SHA-1")
        val hash = digest.digest(stringToSign.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    private suspend fun uploadStreaming(
        context: Context,
        uri: Uri,
        uploadUrl: String,
        resourceType: String,
        folder: String,
        fileSize: Long,
        onProgress: ((Int) -> Unit)?
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext UploadResult.Error(Exception("Cannot open URI: $uri"))

            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val fileName = getFileName(context, uri)

            // Generate timestamp for signature
            val timestamp = (System.currentTimeMillis() / 1000).toString()
            
            // Create parameters for signature
            val signatureParams = mutableMapOf(
                "timestamp" to timestamp,
                "folder" to folder
            )
            
            // Generate signature
            val signature = generateSignature(signatureParams)
            
            Log.d(TAG, "Using signed upload with timestamp: $timestamp")

            val connection = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
                doInput = true
                doOutput = true
                useCaches = false
                requestMethod = "POST"
                setRequestProperty("Connection", "Keep-Alive")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
                setChunkedStreamingMode(0)
            }

            val dos = DataOutputStream(connection.outputStream)

            // API Key (required for signed uploads)
            writeFormField(dos, "api_key", CloudinaryConfig.API_KEY)
            
            // Timestamp
            writeFormField(dos, "timestamp", timestamp)
            
            // Signature
            writeFormField(dos, "signature", signature)

            // Folder
            writeFormField(dos, "folder", folder)

            // File data
            dos.writeBytes("$TWO_HYPHENS$BOUNDARY$LINE_END")
            dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"$LINE_END")
            dos.writeBytes("Content-Type: $mimeType$LINE_END")
            dos.writeBytes(LINE_END)

            // Stream the file in chunks
            val buffer = ByteArray(WRITE_CHUNK_SIZE)
            var totalUploaded = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                dos.write(buffer, 0, bytesRead)
                totalUploaded += bytesRead
                
                if (fileSize > 0) {
                    val progress = ((totalUploaded * 100) / fileSize).toInt()
                    onProgress?.invoke(progress)
                }
            }

            inputStream.close()

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

            Log.d(TAG, "Upload response code: $responseCode")

            if (responseCode != HttpURLConnection.HTTP_OK) {
                // Check if it's a file size limit error
                val errorMsg = if (responseText.contains("File size too large") || 
                                   responseText.contains("10485760") ||
                                   responseCode == 413) {
                    "File too large for upload. File size: ${formatFileSize(fileSize)}"
                } else {
                    "Cloudinary error $responseCode: $responseText"
                }
                return@withContext UploadResult.Error(Exception(errorMsg))
            }

            val json = JSONObject(responseText)
            val secureUrl = json.getString("secure_url")
            val publicId  = json.getString("public_id")

            Log.d(TAG, "Upload successful: $secureUrl")

            UploadResult.Success(
                url = secureUrl,
                publicId = publicId,
                resourceType = resourceType
            )

        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
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
