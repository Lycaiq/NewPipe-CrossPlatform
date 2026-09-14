/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.newpipe.app.player.VideoSurface
import org.koin.compose.koinInject
import net.newpipe.app.player.JVMVideoPlayer

/** En JVM embebemos el reproductor nativo de VLC via SwingPanel */
@Composable
actual fun PlayerSurface(streamUrl: String, modifier: Modifier) {
    val player: JVMVideoPlayer = koinInject()
    VideoSurface(url = streamUrl, modifier = modifier, player = player)
}
