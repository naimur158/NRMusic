package com.nrmusic.app.ui.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts a dark, muted dominant color from an artwork URL for the player background.
 * Falls back to [fallback] while loading or when extraction fails.
 */
@Composable
fun rememberAdaptiveColor(artworkUrl: String?, fallback: Color): State<Color> {
    val context = LocalContext.current
    return produceState(initialValue = fallback, key1 = artworkUrl) {
        if (artworkUrl == null) {
            value = fallback
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            runCatching {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        ?: return@runCatching fallback
                    val palette = Palette.from(bitmap).maximumColorCount(24).generate()
                    // Prefer saturated swatches so the tint clearly reads as the album's color.
                    val swatch = palette.darkVibrantSwatch
                        ?: palette.vibrantSwatch
                        ?: palette.dominantSwatch
                        ?: palette.darkMutedSwatch
                        ?: palette.mutedSwatch
                    val raw = swatch?.let { Color(it.rgb) } ?: fallback
                    // Deepen it a touch so white text stays readable while keeping the hue vivid.
                    lerp(raw, Color(0xFF0B0B0B), 0.30f)
                } else fallback
            }.getOrDefault(fallback)
        }
    }
}
