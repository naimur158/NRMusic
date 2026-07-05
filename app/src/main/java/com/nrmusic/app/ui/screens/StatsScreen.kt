package com.nrmusic.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nrmusic.app.data.history.HistoryStore
import com.nrmusic.app.playback.PlayerConnection
import com.nrmusic.app.ui.components.TrackActionsFactory
import com.nrmusic.app.ui.components.TrackListItem

@Composable
fun StatsScreen(
    player: PlayerConnection,
    actionsFor: TrackActionsFactory,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val history by HistoryStore.data.collectAsStateWithLifecycle()
    val topArtists = history.topArtists.take(10)
    val mostPlayed = history.mostPlayed.take(25)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
                    Text("Listening stats", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
                }
            }

            if (mostPlayed.isEmpty()) {
                item {
                    Box(Modifier.fillMaxSize().padding(48.dp), Alignment.Center) {
                        Text("Play some music and your stats will appear here.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                return@LazyColumn
            }

            item {
                Text("Top artists", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
            }
            items(topArtists) { a ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text(a.artist, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${a.count} plays", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            item {
                Text("Most played songs", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
            }
            items(mostPlayed, key = { it.id }) { track ->
                TrackListItem(
                    track = track,
                    highlighted = player.currentMediaId == track.id,
                    actions = actionsFor(track, null),
                    onClick = { player.playQueue(mostPlayed, mostPlayed.indexOf(track)) }
                )
            }
        }
    }
}
