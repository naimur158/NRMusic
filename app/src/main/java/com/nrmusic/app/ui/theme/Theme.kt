package com.nrmusic.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nrmusic.app.data.settings.Settings
import com.nrmusic.app.data.settings.ThemeMode

val SpotifyGreen = Color(0xFF1DB954)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF282828)
val TextSecondary = Color(0xFFB3B3B3)

private fun darkScheme(accent: Color, background: Color, surface: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color.Black,
    primaryContainer = accent,
    onPrimaryContainer = Color.Black,
    secondary = accent,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = Color.White,
    background = background,
    onBackground = Color.White,
    surface = background,
    onSurface = Color.White,
    surfaceVariant = surface,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = surface,
    surfaceContainerHigh = DarkSurfaceVariant,
    outline = Color(0xFF404040),
)

private fun lightScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent,
    onPrimaryContainer = Color.White,
    secondary = accent,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF121212),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFE6E6E6),
    onSurfaceVariant = Color(0xFF5A5A5A),
    surfaceContainer = Color(0xFFF0F0F0),
    surfaceContainerHigh = Color(0xFFE6E6E6),
    outline = Color(0xFFBDBDBD),
)

// Stronger, more energetic hierarchy for a music app: bolder headlines/titles,
// comfortable body line-height, medium-weight labels.
private val NRTypography: Typography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(lineHeight = 24.sp),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun NRMusicTheme(
    settings: Settings,
    content: @Composable () -> Unit,
) {
    // accentColor is stored as a 0xAARRGGBB long.
    val accent = Color(settings.accentColor)
    val systemDark = isSystemInDarkTheme()
    val scheme = when (settings.themeMode) {
        ThemeMode.LIGHT -> lightScheme(accent)
        ThemeMode.DARK -> darkScheme(accent, DarkBackground, DarkSurface)
        ThemeMode.AMOLED -> darkScheme(accent, Color.Black, Color(0xFF0D0D0D))
        ThemeMode.SYSTEM ->
            if (systemDark) darkScheme(accent, DarkBackground, DarkSurface)
            else lightScheme(accent)
    }
    MaterialTheme(colorScheme = scheme, typography = NRTypography, content = content)
}
