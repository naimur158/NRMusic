package com.nrmusic.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nrmusic.app.data.settings.SettingsStore
import com.nrmusic.app.playback.PlayerConnection
import com.nrmusic.app.ui.NRMusicUi
import com.nrmusic.app.ui.theme.NRMusicTheme

class MainActivity : ComponentActivity() {

    private lateinit var playerConnection: PlayerConnection

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        playerConnection = PlayerConnection(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val settings by SettingsStore.data.collectAsStateWithLifecycle()
            NRMusicTheme(settings = settings) {
                NRMusicUi(player = playerConnection)
            }
        }
    }

    override fun onDestroy() {
        playerConnection.release()
        super.onDestroy()
    }
}
