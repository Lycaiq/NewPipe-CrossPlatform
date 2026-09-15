package net.newpipe.app.di

import org.koin.core.module.Module
import org.koin.dsl.module
import net.newpipe.app.di.settings.provideSettings
import net.newpipe.app.search.JVMSearchRepository
import net.newpipe.app.search.SearchRepository
import net.newpipe.app.player.JVMVideoPlayer
import net.newpipe.app.player.VideoPlayer
import net.newpipe.app.download.JVMDownloadManager
import net.newpipe.app.download.DownloadManager

actual val platformModule: Module = module {
    single { provideSettings() }
    single<SearchRepository> { JVMSearchRepository() }
    single<DownloadManager> { JVMDownloadManager(getOrNull() ?: okhttp3.OkHttpClient()) }
    factory<VideoPlayer> { JVMVideoPlayer() }
}
