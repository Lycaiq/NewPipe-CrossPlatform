package org.schabi.newpipe.settings.export

import android.content.Context
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

object GoogleDriveBackupManager {

    private const val CLIENT_ID = "YOUR_GOOGLE_DRIVE_CLIENT_ID" // Placeholder para el cliente OAuth
    private val client = OkHttpClient()

    // 1. Obtener Device Code
    fun startOAuthDeviceFlow(context: Context, onCodeReceived: (String, String) -> Unit, onTokenReceived: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = "client_id=$CLIENT_ID&scope=https://www.googleapis.com/auth/drive.file"
                    .toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("https://oauth2.googleapis.com/device/code")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Failed to get device code")
                    val json = JSONObject(response.body!!.string())
                    val deviceCode = json.getString("device_code")
                    val userCode = json.getString("user_code")
                    val verificationUrl = json.getString("verification_url")
                    val interval = json.getInt("interval")

                    withContext(Dispatchers.Main) {
                        onCodeReceived(userCode, verificationUrl)
                    }

                    pollForToken(context, deviceCode, interval, onTokenReceived)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error en Google Drive OAuth: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 2. Poll for Token
    private suspend fun pollForToken(context: Context, deviceCode: String, interval: Int, onTokenReceived: (String) -> Unit) {
        var token: String? = null
        while (token == null) {
            delay(interval * 1000L)
            try {
                val body = "client_id=$CLIENT_ID&client_secret=&device_code=$deviceCode&grant_type=urn:ietf:params:oauth:grant-type:device_code"
                    .toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("https://oauth2.googleapis.com/token")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body!!.string())
                        token = json.getString("access_token")
                        withContext(Dispatchers.Main) {
                            onTokenReceived(token!!)
                        }
                    }
                }
            } catch (e: Exception) {
                // Keep polling or handle timeout
            }
        }
    }

    // 3. Upload File
    fun uploadZipToDrive(context: Context, token: String, file: File) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("metadata", "", 
                        "{\"name\": \"newpipe_backup.zip\"}".toRequestBody("application/json".toMediaTypeOrNull()))
                    .addFormDataPart("file", file.name, 
                        file.asRequestBody("application/zip".toMediaTypeOrNull()))
                    .build()

                val request = Request.Builder()
                    .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                    .addHeader("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Toast.makeText(context, "¡Base de datos subida a Google Drive exitosamente!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Error al subir a Google Drive", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Fallo de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

    // 4. Download File
    fun downloadZipFromDrive(context: Context, token: String, outputFile: File, onSuccess: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch files list
                val searchRequest = Request.Builder()
                    .url("https://www.googleapis.com/drive/v3/files?q=name='newpipe_backup.zip'&fields=files(id)")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                var fileId: String? = null
                client.newCall(searchRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body!!.string())
                        val files = json.getJSONArray("files")
                        if (files.length() > 0) {
                            fileId = files.getJSONObject(0).getString("id")
                        }
                    }
                }

                if (fileId == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No se encontró el backup en Drive", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // Download file
                val downloadRequest = Request.Builder()
                    .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                client.newCall(downloadRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        outputFile.outputStream().use { fileOut ->
                            response.body!!.byteStream().copyTo(fileOut)
                        }
                        withContext(Dispatchers.Main) {
                            onSuccess()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error al descargar backup", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Fallo de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
