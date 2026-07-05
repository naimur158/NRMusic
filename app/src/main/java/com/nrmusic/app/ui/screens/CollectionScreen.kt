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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import com.nrmusic.app.data.model.Track
import com.nrmusic.app.data.settings.SettingsStore
import com.nrmusic.app.data.settings.SortMode
import com.nrmusic.app.playback.PlayerConnection
import com.nrmusic.app.ui.components.TrackActionsFactory
import com.nrmusic.app.ui.components.TrackListItem
import com.nrmusic.app.ui.util.label
import com.nrmusic.app.ui.util.sortedBy

/** Actions available on an editable playlist (null for Liked/Downloads/Device). */
data class PlaylistControls(
    val onRename: () -> Unit,
    val onChangeCover: () -> Unit,
    val onDelete: () -> Unit,
    val onRemoveTrack: (Track) -> Unit,
)

@Composable
fun CollectionScreen(
    title: String,
    coverUri: String?,
    tracks: List<Track>,
    player: PlayerConnection,
    actionsFor: TrackActionsFactory,
    onBack: () -> Unit,
    playlistControls: PlaylistControls? = null,
    onSwipeRemove: ((Track) -> Unit)? = null,
    onBatchAddToPlaylist: ((List<Track>) -> Unit)? = null,
    onBatchDownload: ((List<Track>) -> Unit)? = null,
) {
    BackHandler(onBack = onBack)

    var sortMode by remember { mutableStateOf(SettingsStore.current.sortMode) }
    var selection by remember { mutableStateOf(setOf<String>()) }
    val selectionMode = selection.isNotEmpty()
    val sorted = remember(tracks, sortMode) { tracks.sortedBy(sortMode) }

    fun toggle(id: String) {
        selection = if (id in selection) selection - id else selection + id
    }

    BackHandler(enabled = selectionMode) { selection = emptySet() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                if (selectionMode) {
                    SelectionBar(
                        count = selection.size,
                        onClose = { selection = emptySet() },
                        onAddToPlaylist = onBatchAddToPlaylist?.let {
                            { it(sorted.filter { t -> t.id in selection }); selection = emptySet() }
                        },
                        onDownload = onBatchDownload?.let {
                            { it(sorted.filter { t -> t.id in selection }); selection = emptySet() }
                        },
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(Modifier.weight(1f))
                        SortMenu(sortMode) { sortMode = it }
                        if (playlistControls != null) PlaylistOverflow(playlistControls)
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
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
                        val art = com.nrmusic.app.ui.util.hiResArtwork(
                            coverUri ?: tracks.firstOrNull()?.thumbnailUrl
                        )
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
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 16.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${tracks.size} songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { if (sorted.isNotEmpty()) player.playQueue(sorted, 0) },
                            enabled = sorted.isNotEmpty()
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Text("Play", Modifier.padding(start = 6.dp))
                        }
                        OutlinedButton(
                            onClick = { if (sorted.isNotEmpty()) player.shufflePlay(sorted) },
                            enabled = sorted.isNotEmpty()
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null)
                            Text("Shuffle", Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
            if (sorted.isEmpty()) {
                item {
                    com.nrmusic.app.ui.components.EmptyState(
                        icon = Icons.Rounded.MusicNote,
                        title = "Nothing here yet",
                        subtitle = "Songs you add will show up here. Try searching for something you love."
                    )
                }
            } else {
                items(sorted, key = { it.id }) { track ->
                    val removeFromPlaylist: (() -> Unit)? =
                        playlistControls?.let { pc -> { pc.onRemoveTrack(track) } }
                    val row: @Composable () -> Unit = {
                        TrackListItem(
                            track = track,
                            highlighted = player.currentMediaId == track.id,
                            actions = actionsFor(track, removeFromPlaylist),
                            selectionMode = selectionMode,
                            selected = track.id in selection,
                            onLongClick = { toggle(track.id) },
                            onClick = {
                                if (selectionMode) toggle(track.id)
                                else player.playQueue(sorted, sorted.indexOf(track))
                            }
                        )
                    }
                    if (onSwipeRemove != null && !selectionMode) {
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { v ->
                                if (v != SwipeToDismissBoxValue.Settled) { onSwipeRemove(track); true }
                                else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    Modifier.fillMaxSize()
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        ) { row() }
                    } else {
                        row()
                    }
                }
            }
            item { Spacer(Modifier.size(24.dp)) }
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
    onAddToPlaylist: (() -> Unit)?,
    onDownload: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = "Cancel selection")
        }
        Text("$count selected", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        onAddToPlaylist?.let {
            IconButton(onClick = it) { Icon(Icons.Rounded.PlaylistAdd, contentDescription = "Add to playlist") }
        }
        onDownload?.let {
            IconButton(onClick = it) { Icon(Icons.Rounded.Download, contentDescription = "Download") }
        }
    }
}

@Composable
private fun SortMenu(current: SortMode, onSelect: (SortMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.Sort, contentDescription = "Sort")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label() + if (mode == current) "  ✓" else "") },
                    onClick = { onSelect(mode); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun PlaylistOverflow(controls: PlaylistControls) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Playlist options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                onClick = { expanded = false; controls.onRename() }
            )
            DropdownMenuItem(
                text = { Text("Change cover") },
                leadingIcon = { Icon(Icons.Rounded.Image, contentDescription = null) },
                onClick = { expanded = false; controls.onChangeCover() }
            )
            DropdownMenuItem(
                text = { Text("Delete playlist") },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                onClick = { expanded = false; controls.onDelete() }
            )
        }
    }
}
