package com.nrmusic.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.nrmusic.app.data.download.DownloadManager
import com.nrmusic.app.data.history.HistoryStore
import com.nrmusic.app.data.library.LibraryStore
import com.nrmusic.app.data.settings.SettingsStore
import com.nrmusic.app.data.youtube.NewPipeDownloader
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

class NRMusicApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        NewPipe.init(
            NewPipeDownloader(OkHttpClient.Builder().build()),
            Localization("en", "US"),
            ContentCountry("US")
        )
        SettingsStore.init(this)
        LibraryStore.init(this)
        HistoryStore.init(this)
        DownloadManager.init(this)
    }

    // Fade artwork in as it loads, everywhere AsyncImage is used.
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .crossfade(true)
            .crossfade(220)
            .build()
}
