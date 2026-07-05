package com.nrmusic.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.nrmusic.app.data.model.formatDuration
import com.nrmusic.app.playback.PlayerConnection
import com.nrmusic.app.ui.theme.DarkBackground
import com.nrmusic.app.ui.util.rememberAdaptiveColor
import kotlin.math.abs
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    player: PlayerConnection,
    isLiked: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadFraction: Float? = null,
    onToggleLike: () -> Unit,
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenLyrics: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val metadata = player.metadata
    var positionMs by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            if (!dragging) positionMs = player.controller?.currentPosition ?: 0L
            delay(500)
        }
    }

    // Background tints toward the artwork's dominant color.
    val targetColor by rememberAdaptiveColor(
        metadata?.artworkUri?.toString(),
        MaterialTheme.colorScheme.background
    )
    val bgColor by animateColorAsState(targetColor, label = "bg")
    val brush = Brush.verticalGradient(listOf(bgColor, DarkBackground))

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().background(brush)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close")
                }
                Text(
                    "Now playing",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = onOpenSleepTimer) {
                    Icon(
                        Icons.Rounded.Timer,
                        contentDescription = "Sleep timer",
                        tint = if (player.sleepTimerEndAt != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            var swipeAccum by remember { mutableFloatStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (abs(swipeAccum) > 120f) {
                                    if (swipeAccum < 0) player.next() else player.previous()
                                }
                                swipeAccum = 0f
                            }
                        ) { _, dragAmount -> swipeAccum += dragAmount }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (metadata?.artworkUri != null) {
                    AsyncImage(
                        model = metadata.artworkUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        metadata?.title?.toString() ?: "Nothing playing",
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        metadata?.artist?.toString() ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                IconButton(onClick = onToggleLike) {
                    Icon(
                        if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (isDownloading) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        val f = downloadFraction ?: -1f
                        if (f >= 0f) {
                            CircularProgressIndicator(
                                progress = { f },
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                "${(f * 100).toInt()}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                } else {
                    IconButton(onClick = { if (isDownloaded) onRemoveDownload() else onDownload() }) {
                        Icon(
                            if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                            contentDescription = "Download",
                            tint = if (isDownloaded) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            player.error?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))

            val duration = player.durationMs.coerceAtLeast(1L)
            Slider(
                value = if (dragging) dragPosition else positionMs.coerceIn(0, duration).toFloat(),
                onValueChange = { dragging = true; dragPosition = it },
                onValueChangeFinished = {
                    player.seekTo(dragPosition.toLong())
                    positionMs = dragPosition.toLong()
                    dragging = false
                },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    formatDuration((if (dragging) dragPosition.toLong() else positionMs) / 1000),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    formatDuration(player.durationMs / 1000),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { player.toggleShuffle() }) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (player.shuffleEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { player.previous() }, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.size(8.dp))
                FilledIconButton(
                    onClick = { player.togglePlayPause() },
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (player.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (player.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(Modifier.size(8.dp))
                IconButton(onClick = { player.next() }, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { player.cycleRepeat() }) {
                    Icon(
                        if (player.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne
                        else Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if (player.repeatMode != Player.REPEAT_MODE_OFF)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenLyrics) {
                    Icon(
                        Icons.Rounded.Lyrics,
                        contentDescription = "Lyrics",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SpeedControl(player.speed) { player.changeSpeed(it) }
                IconButton(onClick = onOpenQueue) {
                    Icon(
                        Icons.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun SpeedControl(currentSpeed: Float, onSpeed: (Float) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    Box {
        androidx.compose.material3.TextButton(onClick = { expanded = true }) {
            Icon(
                Icons.Rounded.Speed,
                contentDescription = "Playback speed",
                tint = if (currentSpeed != 1.0f) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${"%.2f".format(currentSpeed).trimEnd('0').trimEnd('.')}×",
                color = if (currentSpeed != 1.0f) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            speeds.forEach { s ->
                DropdownMenuItem(
                    text = { Text("${s}×") },
                    onClick = { onSpeed(s); expanded = false }
                )
            }
        }
    }
}
