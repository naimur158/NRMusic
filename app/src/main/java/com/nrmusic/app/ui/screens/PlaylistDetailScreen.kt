package com.nrmusic.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import coil.compose.AsyncImage
import com.nrmusic.app.data.library.Playlist
import com.nrmusic.app.data.model.Track
import com.nrmusic.app.playback.PlayerConnection
import com.nrmusic.app.ui.components.TrackActionsFactory
import com.nrmusic.app.ui.components.TrackListItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    player: PlayerConnection,
    actionsFor: TrackActionsFactory,
    onBack: () -> Unit,
    onRename: () -> Unit,
    onChangeCover: () -> Unit,
    onDelete: () -> Unit,
    onRemoveTrack: (Track) -> Unit,
    onReorder: (List<Track>) -> Unit,
) {
    BackHandler(onBack = onBack)

    // Local copy so drag feels instant; re-synced from the store when not dragging.
    var localTracks by remember(playlist.id) { mutableStateOf(playlist.tracks) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(playlist.tracks) { if (!dragging) localTracks = playlist.tracks }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val f = localTracks.indexOfFirst { it.id == from.key }
        val t = localTracks.indexOfFirst { it.id == to.key }
        if (f != -1 && t != -1) {
            localTracks = localTracks.toMutableList().apply { add(t, removeAt(f)) }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
            item(key = "topbar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(Modifier.weight(1f))
                    PlaylistOverflow(onRename, onChangeCover, onDelete)
                }
            }
            item(key = "header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        val art = playlist.coverUri ?: localTracks.firstOrNull()?.thumbnailUrl
                        if (art != null) {
                            AsyncImage(
                                model = art,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        playlist.name,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 16.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${localTracks.size} songs • long-press a song to reorder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { if (localTracks.isNotEmpty()) player.playQueue(localTracks, 0) },
                            enabled = localTracks.isNotEmpty()
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Text("Play", Modifier.padding(start = 6.dp))
                        }
                        OutlinedButton(
                            onClick = { if (localTracks.isNotEmpty()) player.shufflePlay(localTracks) },
                            enabled = localTracks.isNotEmpty()
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null)
                            Text("Shuffle", Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
            if (localTracks.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "No songs yet. Add some from Search or Home.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                items(localTracks, key = { it.id }) { track ->
                    ReorderableItem(reorderState, key = track.id) { isDragging ->
                        Surface(
                            color = if (isDragging) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.background,
                            shadowElevation = if (isDragging) 6.dp else 0.dp
                        ) {
                            Box(
                                Modifier.longPressDraggableHandle(
                                    onDragStarted = { dragging = true },
                                    onDragStopped = {
                                        dragging = false
                                        onReorder(localTracks)
                                    }
                                )
                            ) {
                                TrackListItem(
                                    track = track,
                                    highlighted = player.currentMediaId == track.id,
                                    actions = actionsFor(track) { onRemoveTrack(track) },
                                    onClick = {
                                        player.playQueue(localTracks, localTracks.indexOf(track))
                                    }
                                )
                            }
                        }
                    }
                }
            }
            item(key = "footer") { Spacer(Modifier.size(24.dp)) }
        }
    }
}

@Composable
private fun PlaylistOverflow(
    onRename: () -> Unit,
    onChangeCover: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Playlist options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                onClick = { expanded = false; onRename() }
            )
            DropdownMenuItem(
                text = { Text("Change cover") },
                leadingIcon = { Icon(Icons.Rounded.Image, contentDescription = null) },
                onClick = { expanded = false; onChangeCover() }
            )
            DropdownMenuItem(
                text = { Text("Delete playlist") },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                onClick = { expanded = false; onDelete() }
            )
        }
    }
}
