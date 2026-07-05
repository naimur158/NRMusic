package com.nrmusic.app

import android.app.Application
import com.nrmusic.app.data.download.DownloadManager
import com.nrmusic.app.data.history.HistoryStore
import com.nrmusic.app.data.library.LibraryStore
import com.nrmusic.app.data.settings.SettingsStore
import com.nrmusic.app.data.youtube.NewPipeDownloader
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

class NRMusicApp : Application() {

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
}
