package com.nrmusic.app.data.history

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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class PlayEvent(val track: Track, val at: Long)

@Serializable
data class ArtistCount(val artist: String, val count: Int)

@Serializable
data class HistoryData(
    /** Newest first, capped. */
    val recentPlays: List<PlayEvent> = emptyList(),
    val playCounts: Map<String, Int> = emptyMap(),
    val trackById: Map<String, Track> = emptyMap(),
    val searchHistory: List<String> = emptyList(),
) {
    /** Distinct recently-played tracks, newest first. */
    val recentTracks: List<Track>
        get() {
            val seen = HashSet<String>()
            val out = ArrayList<Track>()
            for (e in recentPlays) if (seen.add(e.track.id)) out.add(e.track)
            return out
        }

    /** Most played tracks, highest count first. */
    val mostPlayed: List<Track>
        get() = playCounts.entries.sortedByDescending { it.value }
            .mapNotNull { trackById[it.key] }

    fun playCount(trackId: String): Int = playCounts[trackId] ?: 0

    /** Top artists by total plays. */
    val topArtists: List<ArtistCount>
        get() {
            val byArtist = HashMap<String, Int>()
            for ((id, c) in playCounts) {
                val artist = trackById[id]?.artist ?: continue
                byArtist[artist] = (byArtist[artist] ?: 0) + c
            }
            return byArtist.entries.sortedByDescending { it.value }
                .map { ArtistCount(it.key, it.value) }
        }
}

object HistoryStore {
    private const val MAX_RECENT = 200
    private const val MAX_SEARCH = 20

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private lateinit var file: File

    private val _data = MutableStateFlow(HistoryData())
    val data: StateFlow<HistoryData> = _data.asStateFlow()

    fun init(context: Context) {
        file = File(context.filesDir, "history.json")
        scope.launch {
            mutex.withLock {
                if (file.exists()) {
                    runCatching { json.decodeFromString<HistoryData>(file.readText()) }
                        .getOrNull()?.let { _data.value = it }
                }
            }
        }
    }

    private fun persist(next: HistoryData) {
        _data.value = next
        scope.launch { mutex.withLock { runCatching { file.writeText(json.encodeToString(next)) } } }
    }

    fun recordPlay(track: Track) {
        val d = _data.value
        val events = (listOf(PlayEvent(track, System.currentTimeMillis())) + d.recentPlays)
            .take(MAX_RECENT)
        persist(
            d.copy(
                recentPlays = events,
                playCounts = d.playCounts + (track.id to (d.playCounts[track.id] ?: 0) + 1),
                trackById = d.trackById + (track.id to track)
            )
        )
    }

    fun recordSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        val d = _data.value
        val next = (listOf(q) + d.searchHistory.filterNot { it.equals(q, ignoreCase = true) })
            .take(MAX_SEARCH)
        persist(d.copy(searchHistory = next))
    }

    fun clearSearchHistory() = persist(_data.value.copy(searchHistory = emptyList()))

    fun clearHistory() = persist(HistoryData())
}
