package com.nrmusic.app.ui.util

import com.nrmusic.app.data.model.Track
import com.nrmusic.app.data.settings.SortMode

/** Applies a [SortMode] to a track list. CUSTOM keeps the original order. */
fun List<Track>.sortedBy(mode: SortMode): List<Track> = when (mode) {
    SortMode.CUSTOM -> this
    SortMode.TITLE -> sortedBy { it.title.lowercase() }
    SortMode.ARTIST -> sortedBy { it.artist.lowercase() }
    SortMode.DATE_ADDED -> this // preserves insertion order (newest-first collections)
    SortMode.DURATION -> sortedBy { it.durationSec }
}

fun SortMode.label(): String = when (this) {
    SortMode.CUSTOM -> "Custom"
    SortMode.TITLE -> "Title"
    SortMode.ARTIST -> "Artist"
    SortMode.DATE_ADDED -> "Date added"
    SortMode.DURATION -> "Duration"
}
