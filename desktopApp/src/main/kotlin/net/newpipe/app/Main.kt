/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import net.newpipe.app.extractor.JVMDownloader
import org.schabi.newpipe.extractor.NewPipe

/**
 * Entry point para Desktop (JVM).
 * Inicializamos el extractor antes de levantar la ventana — si esto falla
 * no tiene sentido mostrar la UI, así que dejamos que explote limpiamente.
 */
fun main() = application {
    // El extractor necesita un downloader inicializado antes del primer uso.
    // Lo hacemos aquí y no en un ViewModel para que sea síncrono y garantizado.
    NewPipe.init(JVMDownloader.getInstance())

    Window(onCloseRequest = ::exitApplication, title = "NewPipe") {
        App(onCloseRequest = ::exitApplication)
    }
}
