package com.nrmusic.app.data.download

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.nrmusic.app.data.library.DownloadInfo
import com.nrmusic.app.data.library.LibraryStore
import com.nrmusic.app.data.model.Track
import com.nrmusic.app.data.youtube.YouTubeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

sealed interface DownloadState {
    /** fraction in 0..1, or -1 while the stream URL is still being resolved (indeterminate). */
    data class Running(val fraction: Float) : DownloadState
    data class Failed(val reason: String) : DownloadState
}

/**
 * Downloads YouTube tracks to internal storage for offline playback.
 * Completed downloads are recorded in [LibraryStore]; in-flight state is exposed here.
 */
object DownloadManager {

    private const val UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var downloadDir: File
    private var appContext: Context? = null

    private val _states = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val states: StateFlow<Map<String, DownloadState>> = _states.asStateFlow()

    /** Count of downloads currently in progress (for a global indicator). */
    val activeCount: Int get() = _states.value.values.count { it is DownloadState.Running }

    fun init(context: Context) {
        appContext = context.applicationContext
        downloadDir = File(context.filesDir, "downloads").apply { mkdirs() }
    }

    private fun setState(id: String, state: DownloadState?) {
        _states.value = if (state == null) _states.value - id else _states.value + (id to state)
    }

    private fun toast(msg: String) {
        appContext?.let { ctx -> main.post { Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() } }
    }

    fun download(track: Track) {
        if (LibraryStore.isDownloaded(track.id)) return
        // Allow a fresh attempt after a failure; only block if already running.
        if (_states.value[track.id] is DownloadState.Running) return

        setState(track.id, DownloadState.Running(-1f))
        scope.launch {
            runCatching { performDownload(track) }
                .onSuccess { file ->
                    LibraryStore.addDownload(
                        DownloadInfo(trackId = track.id, filePath = file.absolutePath, track = track)
                    )
                    setState(track.id, null)
                }
                .onFailure { e ->
                    setState(track.id, DownloadState.Failed(e.message ?: "Download failed"))
                    toast("Couldn't download \"${track.title}\": ${e.message ?: "unknown error"}")
                }
        }
    }

    /** Clears a failed marker so the row returns to a normal "Download" affordance. */
    fun clearFailed(trackId: String) {
        if (_states.value[trackId] is DownloadState.Failed) setState(trackId, null)
    }

    /** Resolves the audio URL with retries — YouTube resolution is intermittently flaky. */
    private fun resolveWithRetry(videoId: String): String {
        var last: Exception? = null
        repeat(3) { attempt ->
            try {
                return YouTubeRepository.resolveStreamUrlBlocking(videoId)
            } catch (e: Exception) {
                last = e
                Thread.sleep(400L * (attempt + 1))
            }
        }
        throw last ?: IllegalStateException("Could not resolve audio stream")
    }

    private suspend fun performDownload(track: Track): File = withContext(Dispatchers.IO) {
        val streamUrl = resolveWithRetry(track.id)
        val target = File(downloadDir, "${track.id}.audio")
        val tmp = File(downloadDir, "${track.id}.part")

        val response = client.newCall(
            Request.Builder().url(streamUrl).header("User-Agent", UA).build()
        ).execute()

        response.use { resp ->
            val body = resp.body ?: throw IllegalStateException("Empty response")
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            val total = body.contentLength()
            body.byteStream().use { input ->
                tmp.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        val fraction = if (total > 0) downloaded.toFloat() / total else -1f
                        setState(track.id, DownloadState.Running(fraction))
                    }
                }
            }
        }
        if (tmp.length() == 0L) throw IllegalStateException("Downloaded 0 bytes")
        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) throw IllegalStateException("Could not save file")
        target
    }
}
