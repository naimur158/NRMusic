package com.nrmusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nrmusic.app.data.history.HistoryStore
import com.nrmusic.app.data.youtube.AlbumResult
import com.nrmusic.app.data.youtube.ArtistResult
import com.nrmusic.app.data.youtube.YouTubeRepository
import com.nrmusic.app.playback.PlayerConnection
import com.nrmusic.app.ui.AppState
import com.nrmusic.app.ui.SearchFilter
import com.nrmusic.app.ui.components.TrackActionsFactory
import com.nrmusic.app.ui.components.TrackListItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val browseTiles = listOf(
    "Pop" to 0xFFE13300, "Hip hop" to 0xFF1E3264, "Bangla" to 0xFF148A08,
    "Bollywood" to 0xFFDC148C, "K-pop" to 0xFF8D67AB, "Rock" to 0xFFE8115B,
    "Lo-fi" to 0xFF503750, "Workout" to 0xFF777777, "Chill" to 0xFF0D73EC,
    "Party" to 0xFFAF2896, "Sleep" to 0xFF1E3264, "Romance" to 0xFFE1118C,
)

@Composable
fun SearchScreen(
    state: AppState,
    player: PlayerConnection,
    contentPadding: PaddingValues,
    actionsFor: TrackActionsFactory,
    onOpenAlbum: (AlbumResult) -> Unit,
    onOpenArtist: (ArtistResult) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val history by HistoryStore.data.collectAsStateWithLifecycle()

    fun runSearch(filter: SearchFilter) {
        val query = state.searchQuery.trim()
        if (query.isEmpty()) return
        keyboard?.hide()
        state.searchFilter = filter
        state.searchLoading = true
        state.searchError = null
        state.searchPerformed = true
        state.searchSuggestions = emptyList()
        HistoryStore.recordSearch(query)
        scope.launch {
            try {
                when (filter) {
                    SearchFilter.SONGS -> state.searchResults = YouTubeRepository.searchSongs(query)
                    SearchFilter.VIDEOS -> state.searchVideos = YouTubeRepository.searchVideos(query)
                    SearchFilter.ALBUMS -> state.searchAlbums = YouTubeRepository.searchAlbums(query)
                    SearchFilter.ARTISTS -> state.searchArtists = YouTubeRepository.searchArtists(query)
                }
            } catch (e: Exception) {
                state.searchError = e.message ?: "Search failed"
            } finally {
                state.searchLoading = false
            }
        }
    }

    // Debounced suggestions while typing.
    LaunchedEffect(state.searchQuery) {
        val q = state.searchQuery.trim()
        if (q.isEmpty() || state.searchPerformed) {
            state.searchSuggestions = emptyList()
        } else {
            delay(250)
            state.searchSuggestions = runCatching { YouTubeRepository.suggestions(q) }.getOrDefault(emptyList())
        }
    }

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { state.searchQuery = it; state.searchPerformed = false },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Songs, artists, albums…") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        state.searchQuery = ""; state.searchPerformed = false
                        state.searchSuggestions = emptyList()
                    }) { Icon(Icons.Rounded.Close, contentDescription = "Clear") }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { runSearch(state.searchFilter) })
        )

        // Filter chips (only relevant once a search is active)
        if (state.searchPerformed) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchFilter.entries.forEach { f ->
                    FilterChip(
                        selected = state.searchFilter == f,
                        onClick = { runSearch(f) },
                        label = { Text(f.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
        }

        when {
            // Suggestions overlay while typing
            !state.searchPerformed && state.searchSuggestions.isNotEmpty() -> LazyColumn {
                items(state.searchSuggestions) { s ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            state.searchQuery = s; runSearch(state.searchFilter)
                        }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(s, Modifier.padding(start = 12.dp))
                    }
                }
            }

            // Landing page: history + browse grid
            !state.searchPerformed -> BrowseLanding(
                history = history.searchHistory,
                onHistoryClick = { state.searchQuery = it; runSearch(SearchFilter.SONGS) },
                onGenreClick = { state.searchQuery = it; runSearch(SearchFilter.SONGS) }
            )

            state.searchLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

            state.searchError != null -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                Text("Search failed: ${state.searchError}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            else -> SearchResults(state, player, actionsFor, onOpenAlbum, onOpenArtist)
        }
    }
}

@Composable
private fun SearchResults(
    state: AppState,
    player: PlayerConnection,
    actionsFor: TrackActionsFactory,
    onOpenAlbum: (AlbumResult) -> Unit,
    onOpenArtist: (ArtistResult) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        when (state.searchFilter) {
            SearchFilter.SONGS -> items(state.searchResults) { track ->
                TrackListItem(
                    track = track,
                    highlighted = player.currentMediaId == track.id,
                    actions = actionsFor(track, null),
                    onClick = { player.playQueue(state.searchResults, state.searchResults.indexOf(track)) }
                )
            }

            SearchFilter.VIDEOS -> items(state.searchVideos) { track ->
                TrackListItem(
                    track = track,
                    highlighted = player.currentMediaId == track.id,
                    actions = actionsFor(track, null),
                    onClick = { player.playQueue(state.searchVideos, state.searchVideos.indexOf(track)) }
                )
            }

            SearchFilter.ALBUMS -> items(state.searchAlbums) { album ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpenAlbum(album) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        album.thumbnail?.let {
                            AsyncImage(it, null, contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp))
                        }
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(album.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Album • ${album.uploader}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            SearchFilter.ARTISTS -> items(state.searchArtists) { artist ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpenArtist(artist) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(52.dp).clip(androidx.compose.foundation.shape.CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (artist.thumbnail != null) {
                            AsyncImage(artist.thumbnail, null, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(52.dp))
                        } else {
                            Icon(Icons.Rounded.Person, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(artist.name, Modifier.padding(start = 12.dp), maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun BrowseLanding(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onGenreClick: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (history.isNotEmpty()) {
            item(span = { GridItemSpanFull() }) {
                Text("Recent searches", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp))
            }
            items(history, span = { GridItemSpanFull() }) { h ->
                Row(
                    Modifier.fillMaxWidth().clickable { onHistoryClick(h) }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(h, Modifier.padding(start = 12.dp))
                }
            }
        }
        item(span = { GridItemSpanFull() }) {
            Text("Browse moods & genres", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        }
        items(browseTiles) { (name, color) ->
            Box(
                Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(10.dp))
                    .background(Color(color)).clickable { onGenreClick(name) }
                    .padding(12.dp)
            ) {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// Full-width span helper for the grid.
private fun androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.GridItemSpanFull() =
    androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan)

/** Kept for Home genre chips: fills query and searches songs. */
fun runSearchFor(state: AppState, scope: kotlinx.coroutines.CoroutineScope, query: String) {
    state.searchQuery = query
    state.searchFilter = SearchFilter.SONGS
    state.searchLoading = true
    state.searchError = null
    state.searchPerformed = true
    HistoryStore.recordSearch(query)
    scope.launch {
        try {
            state.searchResults = YouTubeRepository.searchSongs(query)
        } catch (e: Exception) {
            state.searchError = e.message ?: "Search failed"
        } finally {
            state.searchLoading = false
        }
    }
}
