package com.nrmusic.app.data.settings

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
enum class ThemeMode { SYSTEM, DARK, AMOLED, LIGHT }

@Serializable
enum class SortMode { CUSTOM, TITLE, ARTIST, DATE_ADDED, DURATION }

@Serializable
data class Settings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentColor: Long = 0xFF1DB954,
    val defaultSpeed: Float = 1.0f,
    val headsetDoubleTapNext: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val onboardingDone: Boolean = false,
    val sortMode: SortMode = SortMode.CUSTOM,
)

object SettingsStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private lateinit var file: File

    private val _data = MutableStateFlow(Settings())
    val data: StateFlow<Settings> = _data.asStateFlow()
    val current: Settings get() = _data.value

    fun init(context: Context) {
        file = File(context.filesDir, "settings.json")
        scope.launch {
            mutex.withLock {
                if (file.exists()) {
                    runCatching { json.decodeFromString<Settings>(file.readText()) }
                        .getOrNull()?.let { _data.value = it }
                }
            }
        }
    }

    fun update(transform: (Settings) -> Settings) {
        val next = transform(_data.value)
        _data.value = next
        scope.launch {
            mutex.withLock { runCatching { file.writeText(json.encodeToString(next)) } }
        }
    }
}
