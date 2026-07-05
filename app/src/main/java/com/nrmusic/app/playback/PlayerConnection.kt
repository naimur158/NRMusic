package com.nrmusic.app.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.nrmusic.app.data.history.HistoryStore
import com.nrmusic.app.data.model.Track
import com.nrmusic.app.data.model.toMediaItem

data class QueueItem(
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUri: Uri?,
)

/**
 * Connects the UI to the PlaybackService and exposes player state as Compose state.
 */
class PlayerConnection(private val context: Context) : Player.Listener {

    private val future: ListenableFuture<MediaController> = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, PlaybackService::class.java))
    ).buildAsync()

    var controller: MediaController? by mutableStateOf(null)
        private set
    var metadata: MediaMetadata? by mutableStateOf(null)
        private set
    var currentMediaId: String? by mutableStateOf(null)
        private set
    var isPlaying: Boolean by mutableStateOf(false)
        private set
    var isBuffering: Boolean by mutableStateOf(false)
        private set
    var durationMs: Long by mutableStateOf(0L)
        private set
    var shuffleEnabled: Boolean by mutableStateOf(false)
        private set
    var repeatMode: Int by mutableStateOf(Player.REPEAT_MODE_OFF)
        private set
    var queue: List<QueueItem> by mutableStateOf(emptyList())
        private set
    var currentIndex: Int by mutableStateOf(0)
        private set
    var speed: Float by mutableStateOf(1.0f)
        private set
    var error: String? by mutableStateOf(null)

    /** Wall-clock time (ms) the sleep timer will fire, or null if inactive. */
    var sleepTimerEndAt: Long? by mutableStateOf(null)

    /** Remembers full Track objects for anything queued this session, so the player
     *  UI can offer like/download for the current song. */
    private val registry = HashMap<String, Track>()

    fun currentTrack(): Track? = currentMediaId?.let { registry[it] }

    private var lastRecordedId: String? = null

    val hasTrack: Boolean get() = metadata?.title != null

    init {
        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(this)
            syncFrom(c)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun syncFrom(c: MediaController) {
        metadata = c.mediaMetadata
        currentMediaId = c.currentMediaItem?.mediaId
        isPlaying = c.isPlaying
        durationMs = c.duration.coerceAtLeast(0)
        shuffleEnabled = c.shuffleModeEnabled
        repeatMode = c.repeatMode
        currentIndex = c.currentMediaItemIndex
        speed = c.playbackParameters.speed
        refreshQueue(c)
    }

    private fun refreshQueue(c: MediaController) {
        queue = (0 until c.mediaItemCount).map { i ->
            val mi = c.getMediaItemAt(i)
            QueueItem(
                mediaId = mi.mediaId,
                title = mi.mediaMetadata.title?.toString() ?: "",
                artist = mi.mediaMetadata.artist?.toString() ?: "",
                artworkUri = mi.mediaMetadata.artworkUri
            )
        }
        currentIndex = c.currentMediaItemIndex
    }

    override fun onEvents(player: Player, events: Player.Events) {
        metadata = player.mediaMetadata
        currentMediaId = player.currentMediaItem?.mediaId
        isPlaying = player.isPlaying
        isBuffering = player.playbackState == Player.STATE_BUFFERING
        shuffleEnabled = player.shuffleModeEnabled
        repeatMode = player.repeatMode
        speed = player.playbackParameters.speed
        if (player.playbackState == Player.STATE_READY) {
            durationMs = player.duration.coerceAtLeast(0)
            error = null
        }
        (player as? MediaController)?.let { refreshQueue(it) }
            ?: run { currentIndex = player.currentMediaItemIndex }

        // Clear a spent sleep timer marker.
        sleepTimerEndAt?.let { if (System.currentTimeMillis() >= it) sleepTimerEndAt = null }

        // Record a play when a new track actually starts.
        val id = currentMediaId
        if (id != null && id != lastRecordedId && isPlaying) {
            registry[id]?.let { HistoryStore.recordPlay(it) }
            lastRecordedId = id
        }
    }

    override fun onPlayerError(playbackError: PlaybackException) {
        error = "Playback failed — skipping may help (${playbackError.errorCodeName})"
    }

    /** Replaces the queue with [tracks] and starts playing from [startIndex]. */
    fun playQueue(tracks: List<Track>, startIndex: Int) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        tracks.forEach { registry[it.id] = it }
        c.shuffleModeEnabled = false
        c.setMediaItems(tracks.map { it.toMediaItem() }, startIndex.coerceIn(0, tracks.size - 1), 0L)
        c.prepare()
        c.play()
    }

    /** Shuffle-play a whole collection from a random start. */
    fun shufflePlay(tracks: List<Track>) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        tracks.forEach { registry[it.id] = it }
        c.setMediaItems(tracks.map { it.toMediaItem() }, tracks.indices.random(), 0L)
        c.shuffleModeEnabled = true
        c.prepare()
        c.play()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.seekToPreviousMediaItem()
    fun seekTo(positionMs: Long) = controller?.seekTo(positionMs)
    fun playIndex(index: Int) = controller?.seekToDefaultPosition(index)

    /** Sets playback speed (0.25x–3x). */
    fun changeSpeed(value: Float) {
        val c = controller ?: return
        val clamped = value.coerceIn(0.25f, 3.0f)
        c.setPlaybackSpeed(clamped)
        speed = clamped
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    /** Cycles OFF -> ALL -> ONE -> OFF. */
    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    /** Sets a sleep timer (minutes; 0 cancels). The service pauses playback when it fires. */
    fun setSleepTimer(minutes: Int) {
        val c = controller ?: return
        c.sendCustomCommand(
            SessionCommand(PlaybackService.CMD_SLEEP_TIMER, Bundle.EMPTY),
            Bundle().apply { putInt(PlaybackService.ARG_MINUTES, minutes) }
        )
        sleepTimerEndAt = if (minutes > 0) System.currentTimeMillis() + minutes * 60_000L else null
    }

    fun release() {
        controller?.removeListener(this)
        MediaController.releaseFuture(future)
    }
}
