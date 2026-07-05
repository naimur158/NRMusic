package com.nrmusic.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nrmusic.app.data.library.LibraryData
import com.nrmusic.app.data.local.LocalMusicRepository
import com.nrmusic.app.data.model.Track
import com.nrmusic.app.playback.PlayerConnection
import com.nrmusic.app.ui.AppState
import com.nrmusic.app.ui.CollectionRef
import com.nrmusic.app.ui.components.PlaylistThumb
import com.nrmusic.app.ui.components.TrackActionsFactory
import com.nrmusic.app.ui.components.TrackListItem

private val audioPermission =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

@Composable
fun LibraryScreen(
    state: AppState,
    library: LibraryData,
    player: PlayerConnection,
    contentPadding: PaddingValues,
    actionsFor: TrackActionsFactory,
    onOpen: (CollectionRef) -> Unit,
    onCreatePlaylist: () -> Unit,
) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(hasPermission) {
        if (hasPermission && !state.localLoaded) {
            state.localTracks = LocalMusicRepository.loadTracks(context)
            state.localLoaded = true
        }
    }

    Column(Modifier.padding(contentPadding)) {
        Text(
            "Your Library",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp)
        )
        OutlinedTextField(
            value = state.libraryQuery,
            onValueChange = { state.libraryQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Find in your library") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (state.libraryQuery.isNotEmpty()) {
                    IconButton(onClick = { state.libraryQuery = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true
        )

        val query = state.libraryQuery.trim()
        if (query.isEmpty()) {
            LibraryRoot(
                state = state,
                library = library,
                hasPermission = hasPermission,
                onRequestPermission = { permissionLauncher.launch(audioPermission) },
                onOpen = onOpen,
                onCreatePlaylist = onCreatePlaylist
            )
        } else {
            LibrarySearchResults(
                query = query,
                library = library,
                localTracks = state.localTracks,
                player = player,
                actionsFor = actionsFor,
                onOpenPlaylist = { onOpen(CollectionRef.PlaylistRef(it)) }
            )
        }
    }
}

@Composable
private fun LibraryRoot(
    state: AppState,
    library: LibraryData,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpen: (CollectionRef) -> Unit,
    onCreatePlaylist: () -> Unit,
) {
    LazyColumn {
        item {
            ShortcutRow(
                icon = Icons.Rounded.Favorite,
                title = "Liked Songs",
                subtitle = "${library.liked.size} songs",
                onClick = { onOpen(CollectionRef.Liked) }
            )
        }
        item {
            ShortcutRow(
                icon = Icons.Rounded.Download,
                title = "Downloads",
                subtitle = "${library.downloads.size} songs • available offline",
                onClick = { onOpen(CollectionRef.Downloads) }
            )
        }
        item {
            ShortcutRow(
                icon = Icons.Rounded.PhoneAndroid,
                title = "On this device",
                subtitle = if (hasPermission) "${state.localTracks.size} songs"
                else "Tap to allow access",
                onClick = {
                    if (hasPermission) onOpen(CollectionRef.Device) else onRequestPermission()
                }
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 24.dp, bottom = 4.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Playlists",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCreatePlaylist) {
                    Icon(Icons.Rounded.Add, contentDescription = "New playlist")
                }
            }
        }
        if (library.playlists.isEmpty()) {
            item {
                Text(
                    "No playlists yet. Tap + to make one.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else {
            items(library.playlists) { pl ->
                PlaylistRow(
                    name = pl.name,
                    cover = pl.effectiveCover,
                    count = pl.tracks.size,
                    onClick = { onOpen(CollectionRef.PlaylistRef(pl.id)) }
                )
            }
        }
    }
}

@Composable
private fun LibrarySearchResults(
    query: String,
    library: LibraryData,
    localTracks: List<Track>,
    player: PlayerConnection,
    actionsFor: TrackActionsFactory,
    onOpenPlaylist: (String) -> Unit,
) {
    val q = query.lowercase()

    val matchingPlaylists = library.playlists.filter { it.name.lowercase().contains(q) }

    // Deduped pool of every song known to the library.
    val pool = LinkedHashMap<String, Track>()
    library.liked.forEach { pool[it.id] = it }
    library.downloads.values.forEach { pool[it.track.id] = it.track }
    library.playlists.forEach { pl -> pl.tracks.forEach { pool[it.id] = it } }
    localTracks.forEach { pool[it.id] = it }
    val matchingSongs = pool.values.filter {
        it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
    }

    if (matchingPlaylists.isEmpty() && matchingSongs.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Nothing in your library matches", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn {
        if (matchingPlaylists.isNotEmpty()) {
            item {
                SectionHeader("Playlists")
            }
            items(matchingPlaylists) { pl ->
                PlaylistRow(
                    name = pl.name,
                    cover = pl.effectiveCover,
                    count = pl.tracks.size,
                    onClick = { onOpenPlaylist(pl.id) }
                )
            }
        }
        if (matchingSongs.isNotEmpty()) {
            item {
                SectionHeader("Songs")
            }
            items(matchingSongs) { track ->
                TrackListItem(
                    track = track,
                    highlighted = player.currentMediaId == track.id,
                    actions = actionsFor(track, null),
                    onClick = { player.playQueue(listOf(track), 0) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun PlaylistRow(
    name: String,
    cover: String?,
    count: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaylistThumb(cover, 56.dp)
        Column(Modifier.padding(start = 12.dp)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "$count songs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShortcutRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
