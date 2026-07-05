package com.nrmusic.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nrmusic.app.data.lyrics.Lyrics
import com.nrmusic.app.data.lyrics.LyricsRepository
import com.nrmusic.app.playback.PlayerConnection
import kotlinx.coroutines.delay

@Composable
fun LyricsScreen(player: PlayerConnection, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)

    val metadata = player.metadata
    val title = metadata?.title?.toString() ?: ""
    val artist = metadata?.artist?.toString() ?: ""

    var lyrics by remember { mutableStateOf<Lyrics?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(title, artist) {
        loading = true
        lyrics = null
        if (title.isNotBlank()) {
            lyrics = LyricsRepository.fetch(title, artist, player.durationMs / 1000)
        }
        loading = false
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close")
                }
                Column(Modifier.padding(start = 8.dp)) {
                    Text("Lyrics", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "$title — $artist",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val current = lyrics
            when {
                loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                current == null || current.isEmpty -> Box(
                    Modifier.fillMaxSize().padding(24.dp), Alignment.Center
                ) {
                    Text(
                        "No lyrics found for this track",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                current.hasSynced -> SyncedLyrics(player, current)
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
                ) {
                    Text(
                        current.plain ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 28.sp
                    )
                    Spacer(Modifier.padding(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SyncedLyrics(player: PlayerConnection, lyrics: Lyrics) {
    val listState = rememberLazyListState()
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(lyrics) {
        while (true) {
            val pos = player.controller?.currentPosition ?: 0L
            val idx = lyrics.synced.indexOfLast { it.timeMs <= pos }.coerceAtLeast(0)
            if (idx != currentIndex) {
                currentIndex = idx
                listState.animateScrollToItem(idx.coerceAtLeast(0), scrollOffset = -300)
            }
            delay(300)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 48.dp)
    ) {
        itemsIndexed(lyrics.synced) { index, line ->
            val active = index == currentIndex
            Text(
                line.text.ifBlank { "♪" },
                style = MaterialTheme.typography.titleMedium,
                color = if (active) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (active) 22.sp else 18.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        }
    }
}
