package com.nrmusic.app.playback

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.nrmusic.app.MainActivity
import com.nrmusic.app.data.library.LibraryStore
import com.nrmusic.app.data.model.toMediaItem
import com.nrmusic.app.data.settings.SettingsStore
import com.nrmusic.app.data.youtube.YouTubeRepository
import java.io.File

@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    private var mediaSession: MediaLibrarySession? = null
    private val handler = Handler(Looper.getMainLooper())
    private var sleepRunnable: Runnable? = null

    // Headset multi-tap detection.
    private var tapCount = 0
    private var tapRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()

        // Data source chain: nrmusic://youtube/{id} URIs resolve to either a downloaded
        // local file (offline) or a fresh googlevideo audio URL at load time. Network
        // resolution is retried a couple of times to ride out transient YouTube hiccups.
        val baseFactory = DefaultDataSource.Factory(this)
        val resolvingFactory = ResolvingDataSource.Factory(baseFactory) { dataSpec ->
            val uri = dataSpec.uri
            if (uri.scheme == "nrmusic") {
                val videoId = uri.lastPathSegment
                    ?: throw IllegalArgumentException("Bad nrmusic uri: $uri")
                val localPath = LibraryStore.downloadPath(videoId)
                if (localPath != null && File(localPath).exists()) {
                    dataSpec.withUri(Uri.fromFile(File(localPath)))
                } else {
                    dataSpec.withUri(Uri.parse(resolveWithRetry(videoId)))
                }
            } else {
                dataSpec
            }
        }

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(resolvingFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.playbackParameters = player.playbackParameters.withSpeed(
            SettingsStore.current.defaultSpeed
        )

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .setSessionActivity(sessionActivity)
            .build()
    }

    private fun resolveWithRetry(videoId: String): String {
        var last: Exception? = null
        repeat(3) { attempt ->
            try {
                return YouTubeRepository.resolveStreamUrlBlocking(videoId)
            } catch (e: Exception) {
                last = e
                Thread.sleep(300L * (attempt + 1))
            }
        }
        throw last ?: IllegalStateException("Could not resolve $videoId")
    }

    private fun setSleepTimer(minutes: Int) {
        sleepRunnable?.let { handler.removeCallbacks(it) }
        sleepRunnable = null
        if (minutes <= 0) return
        val runnable = Runnable { mediaSession?.player?.pause() }
        sleepRunnable = runnable
        handler.postDelayed(runnable, minutes * 60_000L)
    }

    /** Counts rapid taps on a headset button: 1=play/pause, 2=next, 3=previous. */
    private fun handleHeadsetTap(): Boolean {
        if (!SettingsStore.current.headsetDoubleTapNext) return false
        tapCount++
        tapRunnable?.let { handler.removeCallbacks(it) }
        val runnable = Runnable {
            val player = mediaSession?.player
            when {
                tapCount >= 3 -> player?.seekToPreviousMediaItem()
                tapCount == 2 -> player?.seekToNextMediaItem()
                else -> player?.let { if (it.isPlaying) it.pause() else it.play() }
            }
            tapCount = 0
        }
        tapRunnable = runnable
        handler.postDelayed(runnable, 600L)
        return true
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(CMD_SLEEP_TIMER, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == CMD_SLEEP_TIMER) {
                setSleepTimer(args.getInt(ARG_MINUTES, 0))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }

        override fun onMediaButtonEvent(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            intent: Intent
        ): Boolean {
            val keyEvent = if (android.os.Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            }
            // Only the single-button headset hook is multi-tap mapped; the notification's
            // dedicated play/pause and next/prev buttons keep Media3's instant handling.
            if (keyEvent?.action == KeyEvent.ACTION_DOWN &&
                keyEvent.repeatCount == 0 &&
                keyEvent.keyCode == KeyEvent.KEYCODE_HEADSETHOOK
            ) {
                if (handleHeadsetTap()) return true
            }
            return super.onMediaButtonEvent(session, controllerInfo, intent)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(BrowseTree.rootItem(), params))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
            Futures.immediateFuture(LibraryResult.ofItemList(BrowseTree.children(parentId), params))

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val track = BrowseTree.findTrack(mediaId)
                ?: return Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
            return Futures.immediateFuture(
                LibraryResult.ofItem(track.toMediaItem(), /* params = */ null)
            )
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        sleepRunnable?.let { handler.removeCallbacks(it) }
        tapRunnable?.let { handler.removeCallbacks(it) }
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val CMD_SLEEP_TIMER = "com.nrmusic.SET_SLEEP_TIMER"
        const val ARG_MINUTES = "minutes"
    }
}
