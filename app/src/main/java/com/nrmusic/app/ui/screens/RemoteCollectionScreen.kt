package com.nrmusic.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nrmusic.app.data.model.Track
import com.nrmusic.app.playback.PlayerConnection
import com.nrmusic.app.ui.components.TrackActionsFactory

/** Loads a track list asynchronously (artist / album / genre / recommendations) then shows it. */
@Composable
fun RemoteCollectionScreen(
    title: String,
    player: PlayerConnection,
    actionsFor: TrackActionsFactory,
    onBack: () -> Unit,
    loader: suspend () -> List<Track>,
) {
    BackHandler(onBack = onBack)
    var tracks by remember { mutableStateOf<List<Track>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(title, reloadKey) {
        tracks = null
        error = null
        runCatching { loader() }
            .onSuccess { tracks = it }
            .onFailure { error = it.message ?: "Couldn't load" }
    }

    when {
        error != null -> Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                TopBar(title, onBack)
                Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Couldn't load: $error",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { reloadKey++ }, modifier = Modifier.padding(top = 12.dp)) {
                            Text("Retry")
                        }
                    }
                }
            }
        }

        tracks == null -> Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                TopBar(title, onBack)
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            }
        }

        else -> CollectionScreen(
            title = title,
            coverUri = null,
            tracks = tracks!!,
            player = player,
            actionsFor = actionsFor,
            onBack = onBack
        )
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
    }
}
