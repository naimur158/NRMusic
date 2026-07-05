package com.nrmusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nrmusic.app.data.history.HistoryStore
import com.nrmusic.app.data.library.LibraryStore
import com.nrmusic.app.data.model.Track
import com.nrmusic.app.data.youtube.YouTubeRepository
import com.nrmusic.app.playback.PlayerConnection
import com.nrmusic.app.ui.AppState
import com.nrmusic.app.ui.CollectionRef
import com.nrmusic.app.ui.components.TrackActionsFactory
import com.nrmusic.app.ui.components.TrackListItem
import kotlinx.coroutines.launch

private val genres = listOf(
    "Pop hits", "Bangla songs", "Hip hop", "Bollywood", "Lo-fi beats", "Rock classics", "K-pop"
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: AppState,
    player: PlayerConnection,
    contentPadding: PaddingValues,
    actionsFor: TrackActionsFactory,
    onGenreClick: (String) -> Unit,
    onOpenCollection: (CollectionRef) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val history by HistoryStore.data.collectAsStateWithLifecycle()
    val library by LibraryStore.data.collectAsStateWithLifecycle()

    val recent = history.recentTracks.take(12)

    // "Because you liked …" recommendations, seeded from the newest liked song.
    var recs by remember { mutableStateOf<List<Track>>(emptyList()) }
    val seed = library.liked.firstOrNull()
    LaunchedEffect(seed?.id) {
        recs = if (seed != null) {
            runCatching { YouTubeRepository.relatedTo(seed.id) }.getOrDefault(emptyList())
        } else emptyList()
    }

    fun loadTrending() {
        if (state.homeLoading) return
        state.homeLoading = true
        state.homeError = null
        scope.launch {
            try {
                state.homeTracks = YouTubeRepository.trendingMusic()
            } catch (e: Exception) {
                state.homeError = e.message ?: "Failed to load"
            } finally {
                state.homeLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (state.homeTracks.isEmpty() && !state.homeLoading) loadTrending()
    }

    PullToRefreshBox(
        isRefreshing = state.homeLoading,
        onRefresh = { loadTrending() },
        modifier = Modifier.fillMaxSize()
    ) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            Text(
                "Good vibes only 🎵",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Recently played
        if (recent.isNotEmpty()) {
            item { SectionTitle("Jump back in") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recent) { track ->
                        RecentCard(track) { player.playQueue(recent, recent.indexOf(track)) }
                    }
                }
            }
        }

        // Made-for-you smart collections
        item { SectionTitle("Made for you") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(smartCards) { (label, ref) ->
                    SmartCard(label) { onOpenCollection(ref) }
                }
            }
        }

        // Recommendations
        if (recs.isNotEmpty()) {
            item { SectionTitle("Because you liked ${seed?.title ?: ""}") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recs) { track ->
                        RecentCard(track) { player.playQueue(recs, recs.indexOf(track)) }
                    }
                }
            }
        }

        // Genre chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                items(genres) { genre ->
                    AssistChip(onClick = { onGenreClick(genre) }, label = { Text(genre) })
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 24.dp, bottom = 8.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Trending songs", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (state.homeTracks.isNotEmpty()) {
                    FilledTonalButton(onClick = { player.shufflePlay(state.homeTracks) }) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null)
                        Text("Shuffle", Modifier.padding(start = 6.dp))
                    }
                }
            }
        }

        when {
            state.homeLoading -> item {
                Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() }
            }

            state.homeError != null -> item {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Couldn't load: ${state.homeError}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { loadTrending() }, modifier = Modifier.padding(top = 12.dp)) { Text("Retry") }
                }
            }

            else -> items(state.homeTracks) { track ->
                TrackListItem(
                    track = track,
                    highlighted = player.currentMediaId == track.id,
                    actions = actionsFor(track, null),
                    onClick = { player.playQueue(state.homeTracks, state.homeTracks.indexOf(track)) }
                )
            }
        }
    }
    }
}

private val smartCards = listOf(
    "Recently played" to CollectionRef.RecentlyPlayed,
    "Most played" to CollectionRef.MostPlayed,
    "Recently added" to CollectionRef.RecentlyAdded,
)

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun RecentCard(track: Track, onClick: () -> Unit) {
    Column(Modifier.width(120.dp).clickable(onClick = onClick)) {
        Box(
            Modifier.size(120.dp).clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            track.thumbnailUrl?.let {
                AsyncImage(it, null, contentScale = ContentScale.Crop, modifier = Modifier.size(120.dp))
            }
        }
        Text(track.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        Text(track.artist, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SmartCard(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(150.dp, 80.dp).clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick).padding(12.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall)
    }
}
