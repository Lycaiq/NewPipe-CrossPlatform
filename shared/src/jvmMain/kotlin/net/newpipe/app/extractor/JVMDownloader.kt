/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.extractor

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.util.concurrent.TimeUnit

/**
 * Implementación del Downloader del extractor usando OkHttp puro para JVM.
 *
 * El extractor de NewPipe define su propia interfaz Downloader —
 * esta clase es el puente entre esa interfaz y OkHttp que hace las peticiones reales.
 * Es prácticamente igual al DownloaderImpl del app Android pero sin las dependencias de Android.
 */
class JVMDownloader private constructor() : Downloader() {

    val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): Response {
        val requestBuilder = okhttp3.Request.Builder()
            .url(request.url())
            .addHeader("User-Agent", USER_AGENT)

        // Copiamos todos los headers que el extractor nos pide poner
        request.headers().forEach { (key, values) ->
            values.forEach { value -> requestBuilder.addHeader(key, value) }
        }

        val body = request.dataToSend()
        if (body != null) {
            requestBuilder.post(body.toRequestBody())
        } else if (request.httpMethod() == "POST") {
            // POST sin body — OkHttp necesita un body vacío explícito
            requestBuilder.post(ByteArray(0).toRequestBody())
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string() ?: ""

        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            responseBody,
            response.request.url.toString()
        )
    }

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"

        @Volatile
        private var instance: JVMDownloader? = null

        fun getInstance(): JVMDownloader =
            instance ?: synchronized(this) {
                instance ?: JVMDownloader().also { instance = it }
            }
    }
}
