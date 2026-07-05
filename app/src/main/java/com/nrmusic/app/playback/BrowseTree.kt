package com.nrmusic.app.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.google.common.collect.ImmutableList
import com.nrmusic.app.data.history.HistoryStore
import com.nrmusic.app.data.library.LibraryStore
import com.nrmusic.app.data.model.Track
import com.nrmusic.app.data.model.toMediaItem

/** Builds the browsable content tree exposed to Android Auto / system browsers. */
object BrowseTree {

    const val ROOT = "root"
    private const val LIKED = "liked"
    private const val DOWNLOADS = "downloads"
    private const val RECENT = "recent"
    private const val PLAYLISTS = "playlists"
    private const val PLAYLIST_PREFIX = "playlist:"

    private fun browsableItem(id: String, title: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build()
            )
            .build()

    fun rootItem(): MediaItem = browsableItem(ROOT, "NR Music")

    fun children(parentId: String): ImmutableList<MediaItem> = when (parentId) {
        ROOT -> ImmutableList.of(
            browsableItem(RECENT, "Recently played"),
            browsableItem(LIKED, "Liked Songs"),
            browsableItem(DOWNLOADS, "Downloads"),
            browsableItem(PLAYLISTS, "Playlists"),
        )

        RECENT -> tracksToItems(HistoryStore.data.value.recentTracks)
        LIKED -> tracksToItems(LibraryStore.data.value.liked)
        DOWNLOADS -> tracksToItems(LibraryStore.data.value.downloads.values.map { it.track })
        PLAYLISTS -> ImmutableList.copyOf(
            LibraryStore.data.value.playlists.map { browsableItem(PLAYLIST_PREFIX + it.id, it.name) }
        )

        else -> if (parentId.startsWith(PLAYLIST_PREFIX)) {
            val id = parentId.removePrefix(PLAYLIST_PREFIX)
            val pl = LibraryStore.data.value.playlists.firstOrNull { it.id == id }
            tracksToItems(pl?.tracks ?: emptyList())
        } else {
            ImmutableList.of()
        }
    }

    /** Finds a playable track item by media id across all collections. */
    fun findTrack(mediaId: String): Track? {
        val lib = LibraryStore.data.value
        return lib.liked.firstOrNull { it.id == mediaId }
            ?: lib.downloads[mediaId]?.track
            ?: lib.playlists.flatMap { it.tracks }.firstOrNull { it.id == mediaId }
            ?: HistoryStore.data.value.trackById[mediaId]
    }

    private fun tracksToItems(tracks: List<Track>): ImmutableList<MediaItem> =
        ImmutableList.copyOf(tracks.map { it.toMediaItem() })
}
