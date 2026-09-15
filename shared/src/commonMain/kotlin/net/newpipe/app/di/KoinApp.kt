/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.di

import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

import net.newpipe.app.viewmodel.ViewModelModule
import net.newpipe.app.di.settings.SettingsModule
import net.newpipe.app.di.serialization.SerializationModule
import net.newpipe.app.bookmark.BookmarkModule
import net.newpipe.app.platform.PlatformModule
import net.newpipe.app.subscription.SubscriptionModule
import net.newpipe.app.search.SearchModule
import net.newpipe.app.download.DownloadModule
import net.newpipe.app.history.HistoryModule
import net.newpipe.app.player.PlayerModule

@Module(includes = [
    ViewModelModule::class,
    SettingsModule::class,
    SerializationModule::class,
    BookmarkModule::class,
    PlatformModule::class,
    SubscriptionModule::class,
    SearchModule::class,
    DownloadModule::class,
    HistoryModule::class,
    PlayerModule::class
])
@ComponentScan("net.newpipe.app")
class AppModules

/**
 * Entry point for Koin-related configuration
 */
@KoinApplication
@ComponentScan("net.newpipe.app")
class KoinApp
