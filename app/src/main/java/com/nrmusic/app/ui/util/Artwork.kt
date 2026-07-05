package com.nrmusic.app.ui.util

/**
 * Upgrades a thumbnail URL to a higher resolution for large displays (player art, covers).
 * List rows use the small original; only big surfaces need this.
 */
fun hiResArtwork(url: String?): String? {
    if (url.isNullOrBlank()) return url
    return when {
        // YouTube Music album art (googleusercontent / ggpht): =w60-h60-... or =s120 → larger
        url.contains("googleusercontent.com") || url.contains("ggpht.com") ->
            url.replace(Regex("=w\\d+-h\\d+"), "=w720-h720")
                .replace(Regex("=s\\d+"), "=s720")

        // YouTube video thumbnails: /default|mqdefault|hqdefault|sddefault.jpg → sddefault (always exists, sharp)
        url.contains("ytimg.com") ->
            url.replace(
                Regex("/(default|mqdefault|hqdefault|sddefault|maxresdefault)\\.jpg"),
                "/sddefault.jpg"
            )

        else -> url
    }
}
