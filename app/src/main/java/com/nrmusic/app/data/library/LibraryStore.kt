package com.nrmusic.app.data.library

import android.content.Context
import com.nrmusic.app.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Single source of truth for user library data (playlists, liked songs, downloads).
 * Persisted as JSON in internal storage. Exposed as a StateFlow the UI observes.
 */
object LibraryStore {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()
    private lateinit var file: File

    private val _data = MutableStateFlow(LibraryData())
    val data: StateFlow<LibraryData> = _data.asStateFlow()

    fun init(context: Context) {
        file = File(context.filesDir, "library.json")
        scope.launch {
            writeMutex.withLock {
                if (file.exists()) {
                    runCatching { json.decodeFromString<LibraryData>(file.readText()) }
                        .getOrNull()
                        ?.let { _data.value = it }
                }
            }
        }
    }

    private fun update(transform: (LibraryData) -> LibraryData) {
        val newData = transform(_data.value)
        _data.value = newData
        scope.launch {
            writeMutex.withLock {
                runCatching { file.writeText(json.encodeToString(newData)) }
            }
        }
    }

    // ---- Liked songs ----

    fun isLiked(trackId: String): Boolean = _data.value.liked.any { it.id == trackId }

    fun toggleLike(track: Track) = update { data ->
        val exists = data.liked.any { it.id == track.id }
        val liked = if (exists) data.liked.filterNot { it.id == track.id }
        else listOf(track) + data.liked
        data.copy(liked = liked)
    }

    // ---- Playlists ----

    fun createPlaylist(name: String, coverUri: String? = null): String {
        val id = UUID.randomUUID().toString()
        update { data ->
            data.copy(
                playlists = data.playlists + Playlist(
                    id = id,
                    name = name.ifBlank { "New Playlist" },
                    coverUri = coverUri,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        return id
    }

    fun renamePlaylist(id: String, name: String) = update { data ->
        data.copy(playlists = data.playlists.map {
            if (it.id == id) it.copy(name = name.ifBlank { it.name }) else it
        })
    }

    fun setPlaylistCover(id: String, coverUri: String?) = update { data ->
        data.copy(playlists = data.playlists.map {
            if (it.id == id) it.copy(coverUri = coverUri) else it
        })
    }

    fun deletePlaylist(id: String) = update { data ->
        data.copy(playlists = data.playlists.filterNot { it.id == id })
    }

    fun addToPlaylist(playlistId: String, track: Track) = update { data ->
        data.copy(playlists = data.playlists.map { pl ->
            if (pl.id == playlistId && pl.tracks.none { it.id == track.id }) {
                pl.copy(tracks = pl.tracks + track)
            } else pl
        })
    }

    /** Persists a reordered track list for a playlist (used by drag-to-reorder). */
    fun setPlaylistOrder(playlistId: String, tracks: List<Track>) = update { data ->
        data.copy(playlists = data.playlists.map { pl ->
            if (pl.id == playlistId) pl.copy(tracks = tracks) else pl
        })
    }

    fun removeFromPlaylist(playlistId: String, trackId: String) = update { data ->
        data.copy(playlists = data.playlists.map { pl ->
            if (pl.id == playlistId) pl.copy(tracks = pl.tracks.filterNot { it.id == trackId })
            else pl
        })
    }

    // ---- Downloads ----

    fun isDownloaded(trackId: String): Boolean = _data.value.downloads.containsKey(trackId)

    fun downloadPath(trackId: String): String? = _data.value.downloads[trackId]?.filePath

    fun addDownload(info: DownloadInfo) = update { data ->
        data.copy(downloads = data.downloads + (info.trackId to info))
    }

    fun removeDownload(trackId: String) {
        _data.value.downloads[trackId]?.let { runCatching { File(it.filePath).delete() } }
        update { data -> data.copy(downloads = data.downloads - trackId) }
    }
}
