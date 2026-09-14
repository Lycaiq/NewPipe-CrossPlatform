/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

/**
 * Módulo de Koin para todo lo relacionado con reproducción de medios.
 * El @ComponentScan sobre este package encuentra la implementación
 * de VideoPlayer de cada plataforma automáticamente (JVMVideoPlayer, etc.).
 */
@Module
@ComponentScan
@Configuration
object PlayerModule
