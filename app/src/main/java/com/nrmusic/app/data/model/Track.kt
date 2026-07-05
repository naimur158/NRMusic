package com.nrmusic.app.data.model

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.serialization.Serializable

@Serializable
enum class TrackSource { YOUTUBE, LOCAL }

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val durationSec: Long,
    val thumbnailUrl: String?,
    val source: TrackSource,
    /** For YouTube tracks: nrmusic://youtube/{videoId} (resolved lazily at play time).
     *  For local tracks: a content:// URI. */
    val mediaUri: String,
)

fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id)
    .setUri(mediaUri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(thumbnailUrl?.let { Uri.parse(it) })
            .build()
    )
    .build()

fun formatDuration(totalSec: Long): String {
    if (totalSec <= 0) return "--:--"
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
