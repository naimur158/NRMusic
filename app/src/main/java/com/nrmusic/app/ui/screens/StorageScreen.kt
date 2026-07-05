package com.nrmusic.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nrmusic.app.data.library.LibraryStore
import java.io.File

@Composable
fun StorageScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val library by LibraryStore.data.collectAsStateWithLifecycle()
    val downloads = library.downloads.values.toList()

    val sizes = downloads.associate { it.trackId to (runCatching { File(it.filePath).length() }.getOrDefault(0L)) }
    val totalBytes = sizes.values.sum()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
                    Text("Storage", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
                }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatBytes(totalBytes), style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "${downloads.size} downloaded songs",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (downloads.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { downloads.forEach { LibraryStore.removeDownload(it.trackId) } },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                            Text("Clear all downloads", Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
            if (downloads.isEmpty()) {
                item {
                    Text(
                        "No downloads yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                items(downloads, key = { it.trackId }) { d ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(d.track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${d.track.artist} • ${formatBytes(sizes[d.trackId] ?: 0L)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { LibraryStore.removeDownload(d.trackId) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) "%.2f GB".format(mb / 1024) else "%.1f MB".format(mb)
}
