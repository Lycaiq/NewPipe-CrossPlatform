/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.search

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

/**
 * Módulo de Koin para búsqueda y resolución de streams.
 * El @ComponentScan pica automáticamente JVMSearchRepository en jvmMain.
 */
@Module
@ComponentScan
@Configuration
object SearchModule
