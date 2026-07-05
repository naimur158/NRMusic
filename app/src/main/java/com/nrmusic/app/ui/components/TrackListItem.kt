package com.nrmusic.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistRemove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nrmusic.app.data.model.Track
import com.nrmusic.app.data.model.formatDuration

/** Builds the context-menu actions for a track; the second arg adds a
 *  "Remove from playlist" entry when non-null. */
typealias TrackActionsFactory = (Track, (() -> Unit)?) -> TrackActions

/** Per-track context-menu actions. Any null callback hides that entry. */
data class TrackActions(
    val isLiked: Boolean,
    val isDownloaded: Boolean,
    /** null = not downloading; -1f = resolving (indeterminate); 0f..1f = progress. */
    val downloadFraction: Float?,
    val downloadFailed: Boolean,
    val onToggleLike: () -> Unit,
    val onDownload: () -> Unit,
    val onRemoveDownload: () -> Unit,
    val onAddToPlaylist: () -> Unit,
    val onRemoveFromPlaylist: (() -> Unit)? = null,
) {
    val isDownloading: Boolean get() = downloadFraction != null
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackListItem(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    actions: TrackActions? = null,
    onLongClick: (() -> Unit)? = null,
    selectionMode: Boolean = false,
    selected: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else androidx.compose.ui.graphics.Color.Transparent
            )
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Icon(
                if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (track.thumbnailUrl != null) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(52.dp)
                )
            } else {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                track.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (highlighted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        when {
            actions?.isDownloading == true -> {
                val fraction = actions.downloadFraction ?: -1f
                Box(
                    modifier = Modifier.size(22.dp).padding(end = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (fraction >= 0f) {
                        CircularProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            "${(fraction * 100).toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            actions?.downloadFailed == true -> Icon(
                Icons.Rounded.ErrorOutline,
                contentDescription = "Download failed — tap ⋮ to retry",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp).padding(end = 2.dp)
            )

            actions?.isDownloaded == true -> Icon(
                Icons.Rounded.DownloadDone,
                contentDescription = "Downloaded",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp).padding(end = 2.dp)
            )
        }
        Text(
            formatDuration(track.durationSec),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (selectionMode) {
            Box(Modifier.size(8.dp))
        } else if (actions != null) {
            TrackMenu(actions)
        } else {
            Box(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun TrackMenu(actions: TrackActions) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(if (actions.isLiked) "Remove from Liked" else "Add to Liked") },
                leadingIcon = {
                    Icon(
                        if (actions.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null
                    )
                },
                onClick = { expanded = false; actions.onToggleLike() }
            )
            DropdownMenuItem(
                text = { Text("Add to playlist") },
                leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, contentDescription = null) },
                onClick = { expanded = false; actions.onAddToPlaylist() }
            )
            when {
                actions.isDownloaded -> DropdownMenuItem(
                    text = { Text("Remove download") },
                    leadingIcon = { Icon(Icons.Rounded.DownloadDone, contentDescription = null) },
                    onClick = { expanded = false; actions.onRemoveDownload() }
                )

                actions.isDownloading -> {
                    val f = actions.downloadFraction ?: -1f
                    DropdownMenuItem(
                        text = { Text(if (f >= 0f) "Downloading ${(f * 100).toInt()}%" else "Starting download…") },
                        leadingIcon = {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        },
                        enabled = false,
                        onClick = { }
                    )
                }

                actions.downloadFailed -> DropdownMenuItem(
                    text = { Text("Retry download") },
                    leadingIcon = {
                        Icon(Icons.Rounded.ErrorOutline, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error)
                    },
                    onClick = { expanded = false; actions.onDownload() }
                )

                else -> DropdownMenuItem(
                    text = { Text("Download") },
                    leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                    onClick = { expanded = false; actions.onDownload() }
                )
            }
            actions.onRemoveFromPlaylist?.let { remove ->
                DropdownMenuItem(
                    text = { Text("Remove from playlist") },
                    leadingIcon = { Icon(Icons.Rounded.PlaylistRemove, contentDescription = null) },
                    onClick = { expanded = false; remove() }
                )
            }
        }
    }
}
