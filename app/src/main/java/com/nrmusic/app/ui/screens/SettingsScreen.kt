package com.nrmusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nrmusic.app.BuildConfig
import com.nrmusic.app.data.settings.SettingsStore
import com.nrmusic.app.data.settings.ThemeMode

private val accentSwatches = listOf(
    0xFF1DB954, 0xFF2196F3, 0xFFE91E63, 0xFFFF9800, 0xFF9C27B0, 0xFFFF5252, 0xFF00BCD4
)

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onOpenStorage: () -> Unit,
    onOpenStats: () -> Unit,
    onCheckUpdates: () -> Unit,
) {
    val settings by SettingsStore.data.collectAsStateWithLifecycle()

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Section("Appearance")
        Text("Theme", style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.themeMode == mode,
                    onClick = { SettingsStore.update { it.copy(themeMode = mode) } },
                    label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
        Spacer(Modifier.size(16.dp))
        Text("Accent color", style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            accentSwatches.forEach { argb ->
                val selected = settings.accentColor == argb
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(argb))
                        .border(
                            width = if (selected) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onBackground,
                            shape = CircleShape
                        )
                        .clickable { SettingsStore.update { it.copy(accentColor = argb) } },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
                }
            }
        }

        Section("Playback")
        Text("Default speed: ${"%.2f".format(settings.defaultSpeed).trimEnd('0').trimEnd('.')}×",
            style = MaterialTheme.typography.titleSmall)
        Slider(
            value = settings.defaultSpeed,
            onValueChange = { SettingsStore.update { s -> s.copy(defaultSpeed = it) } },
            valueRange = 0.5f..2.0f,
            steps = 5
        )
        ToggleRow(
            "Headset double-tap = next",
            settings.headsetDoubleTapNext
        ) { v -> SettingsStore.update { it.copy(headsetDoubleTapNext = v) } }
        ToggleRow(
            "Haptic feedback",
            settings.hapticsEnabled
        ) { v -> SettingsStore.update { it.copy(hapticsEnabled = v) } }

        Section("Library")
        NavRow("Storage", "Manage downloads & space", Icons.Rounded.Storage, onOpenStorage)
        NavRow("Listening stats", "Your top songs & artists", Icons.Rounded.BarChart, onOpenStats)

        Section("About")
        Text("NR Music ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(
            onClick = onCheckUpdates,
            modifier = Modifier.padding(top = 8.dp)
        ) { Text("Check for updates") }
        OutlinedButton(
            onClick = { SettingsStore.update { it.copy(onboardingDone = false) } },
            modifier = Modifier.padding(top = 8.dp)
        ) { Text("Replay welcome tour") }

        Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NavRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.padding(start = 16.dp)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
