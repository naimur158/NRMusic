package com.nrmusic.app.data.library

import com.nrmusic.app.data.model.Track
import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    /** Optional custom cover image (persisted content:// or file:// uri). */
    val coverUri: String? = null,
    val tracks: List<Track> = emptyList(),
    val createdAt: Long = 0L,
) {
    /** Cover to show: custom image, else first track's artwork. */
    val effectiveCover: String? get() = coverUri ?: tracks.firstOrNull()?.thumbnailUrl
}

@Serializable
data class DownloadInfo(
    val trackId: String,
    val filePath: String,
    val track: Track,
)

@Serializable
data class LibraryData(
    val playlists: List<Playlist> = emptyList(),
    /** Liked songs, newest first. */
    val liked: List<Track> = emptyList(),
    /** trackId -> completed download. */
    val downloads: Map<String, DownloadInfo> = emptyMap(),
)
