package com.nrmusic.app.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nrmusic.app.data.download.DownloadManager
import com.nrmusic.app.data.download.DownloadState
import com.nrmusic.app.data.history.HistoryStore
import com.nrmusic.app.data.library.LibraryStore
import com.nrmusic.app.data.model.Track
import com.nrmusic.app.data.settings.SettingsStore
import com.nrmusic.app.data.update.UpdateChecker
import com.nrmusic.app.data.update.UpdateManager
import com.nrmusic.app.data.update.UpdateResult
import com.nrmusic.app.data.youtube.AlbumResult
import com.nrmusic.app.data.youtube.ArtistResult
import com.nrmusic.app.data.youtube.YouTubeRepository
import com.nrmusic.app.playback.PlayerConnection
import com.nrmusic.app.ui.components.AddToPlaylistSheet
import com.nrmusic.app.ui.components.CreatePlaylistDialog
import com.nrmusic.app.ui.components.MiniPlayer
import com.nrmusic.app.ui.components.QueueSheet
import com.nrmusic.app.ui.components.RenamePlaylistDialog
import com.nrmusic.app.ui.components.SleepTimerSheet
import com.nrmusic.app.ui.components.TrackActions
import com.nrmusic.app.ui.components.TrackActionsFactory
import com.nrmusic.app.ui.screens.CollectionScreen
import com.nrmusic.app.ui.screens.HomeScreen
import com.nrmusic.app.ui.screens.LibraryScreen
import com.nrmusic.app.ui.screens.LyricsScreen
import com.nrmusic.app.ui.screens.OnboardingScreen
import com.nrmusic.app.ui.screens.PlayerScreen
import com.nrmusic.app.ui.screens.PlaylistDetailScreen
import com.nrmusic.app.ui.screens.RemoteCollectionScreen
import com.nrmusic.app.ui.screens.SearchScreen
import com.nrmusic.app.ui.screens.SettingsScreen
import com.nrmusic.app.ui.screens.StatsScreen
import com.nrmusic.app.ui.screens.StorageScreen
import com.nrmusic.app.ui.screens.runSearchFor
import kotlinx.coroutines.launch

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Rounded.Home),
    Search("Search", Icons.Rounded.Search),
    Library("Library", Icons.Rounded.LibraryMusic),
    Settings("Settings", Icons.Rounded.Settings),
}

private data class RemoteSpec(val title: String, val loader: suspend () -> List<Track>)

@Composable
fun NRMusicUi(player: PlayerConnection) {
    val state = remember { AppState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val library by LibraryStore.data.collectAsStateWithLifecycle()
    val history by HistoryStore.data.collectAsStateWithLifecycle()
    val settings by SettingsStore.data.collectAsStateWithLifecycle()
    val downloadStates by DownloadManager.states.collectAsStateWithLifecycle()
    val online = rememberOnline()

    var currentTab by remember { mutableStateOf(Tab.Home) }

    // Back from a non-Home tab returns to Home first (instead of exiting the app).
    // Overlays register their own BackHandlers later, so they take priority when open.
    BackHandler(enabled = currentTab != Tab.Home) { currentTab = Tab.Home }

    var showPlayer by remember { mutableStateOf(false) }
    var openCollection by remember { mutableStateOf<CollectionRef?>(null) }
    var remoteSpec by remember { mutableStateOf<RemoteSpec?>(null) }

    // Sheets / dialogs
    var playlistAddTargets by remember { mutableStateOf<List<Track>?>(null) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var pendingTracksForNewPlaylist by remember { mutableStateOf<List<Track>>(emptyList()) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showStorage by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var renamePlaylistId by remember { mutableStateOf<String?>(null) }
    var coverTargetPlaylistId by remember { mutableStateOf<String?>(null) }

    // In-app update state
    var updateAvailable by remember { mutableStateOf<UpdateResult.Available?>(null) }
    var updateProgress by remember { mutableFloatStateOf(-1f) } // -1 = not downloading

    fun runUpdateCheck(manual: Boolean) {
        if (!UpdateChecker.isConfigured) {
            if (manual) Toast.makeText(context, "Updates are set up once you publish via GitHub", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            when (val r = UpdateChecker.check()) {
                is UpdateResult.Available -> updateAvailable = r
                is UpdateResult.UpToDate -> if (manual)
                    Toast.makeText(context, "You're on the latest version", Toast.LENGTH_SHORT).show()
                is UpdateResult.Failed -> if (manual)
                    Toast.makeText(context, "Update check failed: ${r.reason}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) { runUpdateCheck(manual = false) }

    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = coverTargetPlaylistId
        if (uri != null && target != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            LibraryStore.setPlaylistCover(target, uri.toString())
        }
        coverTargetPlaylistId = null
    }

    val actionsFor: TrackActionsFactory = { track, onRemove ->
        val st = downloadStates[track.id]
        TrackActions(
            isLiked = library.liked.any { it.id == track.id },
            isDownloaded = library.downloads.containsKey(track.id),
            downloadFraction = (st as? DownloadState.Running)?.fraction,
            downloadFailed = st is DownloadState.Failed,
            onToggleLike = { LibraryStore.toggleLike(track) },
            onDownload = { DownloadManager.download(track) },
            onRemoveDownload = { LibraryStore.removeDownload(track.id) },
            onAddToPlaylist = { playlistAddTargets = listOf(track) },
            onRemoveFromPlaylist = onRemove,
        )
    }

    Scaffold(
        topBar = {
            if (!online) {
                Text(
                    "Offline — streaming unavailable, downloads still play",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(6.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        bottomBar = {
            Column {
                if (player.hasTrack) MiniPlayer(player = player, onClick = { showPlayer = true })
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding: PaddingValues ->
        when (currentTab) {
            Tab.Home -> HomeScreen(
                state = state,
                player = player,
                contentPadding = padding,
                actionsFor = actionsFor,
                onGenreClick = { genre -> runSearchFor(state, scope, genre); currentTab = Tab.Search },
                onOpenCollection = { openCollection = it }
            )

            Tab.Search -> SearchScreen(
                state = state,
                player = player,
                contentPadding = padding,
                actionsFor = actionsFor,
                onOpenAlbum = { album ->
                    remoteSpec = RemoteSpec(album.name) { YouTubeRepository.albumTracks(album.url) }
                },
                onOpenArtist = { artist ->
                    remoteSpec = RemoteSpec(artist.name) { YouTubeRepository.artistSongs(artist.name) }
                }
            )

            Tab.Library -> LibraryScreen(
                state = state,
                library = library,
                player = player,
                contentPadding = padding,
                actionsFor = actionsFor,
                onOpen = { openCollection = it },
                onCreatePlaylist = { pendingTracksForNewPlaylist = emptyList(); showCreatePlaylist = true }
            )

            Tab.Settings -> SettingsScreen(
                contentPadding = padding,
                onOpenStorage = { showStorage = true },
                onOpenStats = { showStats = true },
                onCheckUpdates = { runUpdateCheck(manual = true) }
            )
        }
    }

    // ---- Full-screen collection overlay ----
    openCollection?.let { ref ->
        when (ref) {
            is CollectionRef.PlaylistRef -> {
                library.playlists.firstOrNull { it.id == ref.id }?.let { pl ->
                    PlaylistDetailScreen(
                        playlist = pl,
                        player = player,
                        actionsFor = actionsFor,
                        onBack = { openCollection = null },
                        onRename = { renamePlaylistId = pl.id },
                        onChangeCover = {
                            coverTargetPlaylistId = pl.id
                            coverPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onDelete = { LibraryStore.deletePlaylist(pl.id); openCollection = null },
                        onRemoveTrack = { LibraryStore.removeFromPlaylist(pl.id, it.id) },
                        onReorder = { LibraryStore.setPlaylistOrder(pl.id, it) }
                    )
                }
            }

            else -> {
                val title: String
                val tracks: List<Track>
                var swipeRemove: ((Track) -> Unit)? = null
                when (ref) {
                    CollectionRef.Liked -> {
                        title = "Liked Songs"; tracks = library.liked
                        swipeRemove = { LibraryStore.toggleLike(it) }
                    }
                    CollectionRef.Downloads -> {
                        title = "Downloads"; tracks = library.downloads.values.map { it.track }
                        swipeRemove = { LibraryStore.removeDownload(it.id) }
                    }
                    CollectionRef.Device -> { title = "On this device"; tracks = state.localTracks }
                    CollectionRef.RecentlyPlayed -> { title = "Recently played"; tracks = history.recentTracks }
                    CollectionRef.MostPlayed -> { title = "Most played"; tracks = history.mostPlayed }
                    CollectionRef.RecentlyAdded -> { title = "Recently added"; tracks = library.liked }
                    is CollectionRef.PlaylistRef -> { title = ""; tracks = emptyList() }
                }
                CollectionScreen(
                    title = title,
                    coverUri = null,
                    tracks = tracks,
                    player = player,
                    actionsFor = actionsFor,
                    onBack = { openCollection = null },
                    onSwipeRemove = swipeRemove,
                    onBatchAddToPlaylist = { playlistAddTargets = it },
                    onBatchDownload = { list -> list.forEach { DownloadManager.download(it) } }
                )
            }
        }
    }

    // ---- Remote (artist/album/recommendations) overlay ----
    remoteSpec?.let { spec ->
        RemoteCollectionScreen(
            title = spec.title,
            player = player,
            actionsFor = actionsFor,
            onBack = { remoteSpec = null },
            loader = spec.loader
        )
    }

    // ---- Full-screen player overlay ----
    AnimatedVisibility(
        visible = showPlayer,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        val current = player.currentTrack()
        val downloaded = current != null && library.downloads.containsKey(current.id)
        val st = current?.let { downloadStates[it.id] }
        val downloading = st is DownloadState.Running
        PlayerScreen(
            player = player,
            isLiked = current != null && library.liked.any { it.id == current.id },
            isDownloaded = downloaded,
            isDownloading = downloading,
            downloadFraction = (st as? DownloadState.Running)?.fraction,
            onToggleLike = { current?.let { LibraryStore.toggleLike(it) } },
            onDownload = { current?.let { DownloadManager.download(it) } },
            onRemoveDownload = { current?.let { LibraryStore.removeDownload(it.id) } },
            onOpenQueue = { showQueue = true },
            onOpenSleepTimer = { showSleepTimer = true },
            onOpenLyrics = { showLyrics = true },
            onDismiss = { showPlayer = false }
        )
    }

    // ---- Player sub-screens ----
    AnimatedVisibility(showLyrics, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
        LyricsScreen(player = player, onDismiss = { showLyrics = false })
    }
    AnimatedVisibility(showStorage, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
        StorageScreen(onBack = { showStorage = false })
    }
    AnimatedVisibility(showStats, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
        StatsScreen(player = player, actionsFor = actionsFor, onBack = { showStats = false })
    }

    // ---- Sheets & dialogs ----
    playlistAddTargets?.let { targets ->
        AddToPlaylistSheet(
            playlists = library.playlists,
            onCreateNew = {
                pendingTracksForNewPlaylist = targets
                playlistAddTargets = null
                showCreatePlaylist = true
            },
            onPick = { playlistId ->
                targets.forEach { LibraryStore.addToPlaylist(playlistId, it) }
                playlistAddTargets = null
            },
            onDismiss = { playlistAddTargets = null }
        )
    }

    if (showCreatePlaylist) {
        CreatePlaylistDialog(
            onConfirm = { name ->
                val id = LibraryStore.createPlaylist(name)
                pendingTracksForNewPlaylist.forEach { LibraryStore.addToPlaylist(id, it) }
                pendingTracksForNewPlaylist = emptyList()
                showCreatePlaylist = false
            },
            onDismiss = { showCreatePlaylist = false; pendingTracksForNewPlaylist = emptyList() }
        )
    }

    renamePlaylistId?.let { id ->
        val name = library.playlists.firstOrNull { it.id == id }?.name ?: ""
        RenamePlaylistDialog(
            initialName = name,
            onConfirm = { LibraryStore.renamePlaylist(id, it); renamePlaylistId = null },
            onDismiss = { renamePlaylistId = null }
        )
    }

    if (showSleepTimer) {
        SleepTimerSheet(
            activeMinutesLabel = player.sleepTimerEndAt?.let {
                val mins = ((it - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0)
                "$mins min"
            },
            onSelect = { minutes -> player.setSleepTimer(minutes); showSleepTimer = false },
            onDismiss = { showSleepTimer = false }
        )
    }

    if (showQueue) {
        QueueSheet(
            queue = player.queue,
            currentIndex = player.currentIndex,
            onPlayIndex = { player.playIndex(it) },
            onDismiss = { showQueue = false }
        )
    }

    // ---- Update available dialog ----
    updateAvailable?.let { update ->
        val downloading = updateProgress >= 0f
        AlertDialog(
            onDismissRequest = { if (!downloading) updateAvailable = null },
            title = { Text("Update available") },
            text = {
                Column {
                    Text("${update.versionName} is ready to install.")
                    if (update.notes.isNotBlank()) {
                        Text(
                            update.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    if (downloading) {
                        LinearProgressIndicator(
                            progress = { updateProgress },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        )
                        Text(
                            "Downloading ${(updateProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !downloading,
                    onClick = {
                        updateProgress = 0f
                        scope.launch {
                            val err = UpdateManager.downloadAndInstall(context, update.apkUrl) {
                                updateProgress = it
                            }
                            updateProgress = -1f
                            if (err != null) {
                                Toast.makeText(context, "Update failed: $err", Toast.LENGTH_LONG).show()
                            } else {
                                updateAvailable = null
                            }
                        }
                    }
                ) { Text(if (downloading) "Downloading…" else "Update now") }
            },
            dismissButton = {
                TextButton(enabled = !downloading, onClick = { updateAvailable = null }) { Text("Later") }
            }
        )
    }

    // ---- First-run onboarding (above everything) ----
    if (!settings.onboardingDone) {
        OnboardingScreen(onDone = { SettingsStore.update { it.copy(onboardingDone = true) } })
    }
}

/** Live connectivity state. */
@Composable
private fun rememberOnline(): Boolean {
    val context = LocalContext.current
    val cm = remember { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    var online by remember { mutableStateOf(cm.activeNetwork != null) }
    DisposableEffect(Unit) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { online = true }
            override fun onLost(network: Network) { online = cm.activeNetwork != null }
        }
        runCatching { cm.registerDefaultNetworkCallback(callback) }
        onDispose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }
    return online
}
