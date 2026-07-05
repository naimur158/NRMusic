package com.nrmusic.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nrmusic.app.data.model.Track
import com.nrmusic.app.data.youtube.AlbumResult
import com.nrmusic.app.data.youtube.ArtistResult

enum class SearchFilter { SONGS, VIDEOS, ALBUMS, ARTISTS }

/** Which track collection a full-screen [CollectionScreen] is showing. */
sealed interface CollectionRef {
    data object Liked : CollectionRef
    data object Downloads : CollectionRef
    data object Device : CollectionRef
    data object RecentlyPlayed : CollectionRef
    data object MostPlayed : CollectionRef
    data object RecentlyAdded : CollectionRef
    data class PlaylistRef(val id: String) : CollectionRef
}

/** UI state that survives tab switches (search results, loaded lists, etc.). */
class AppState {
    var homeTracks by mutableStateOf<List<Track>>(emptyList())
    var homeLoading by mutableStateOf(false)
    var homeError by mutableStateOf<String?>(null)

    var searchQuery by mutableStateOf("")
    var searchResults by mutableStateOf<List<Track>>(emptyList())
    var searchLoading by mutableStateOf(false)
    var searchError by mutableStateOf<String?>(null)
    var searchPerformed by mutableStateOf(false)
    var searchFilter by mutableStateOf(SearchFilter.SONGS)
    var searchVideos by mutableStateOf<List<Track>>(emptyList())
    var searchAlbums by mutableStateOf<List<AlbumResult>>(emptyList())
    var searchArtists by mutableStateOf<List<ArtistResult>>(emptyList())
    var searchSuggestions by mutableStateOf<List<String>>(emptyList())

    var localTracks by mutableStateOf<List<Track>>(emptyList())
    var localLoaded by mutableStateOf(false)

    var libraryQuery by mutableStateOf("")
}
