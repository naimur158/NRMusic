package com.nrmusic.app.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.nrmusic.app.data.model.Track
import com.nrmusic.app.data.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LocalMusicRepository {

    private val albumArtBaseUri: Uri = Uri.parse("content://media/external/audio/albumart")

    suspend fun loadTracks(context: Context): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val albumId = cursor.getLong(4)
                tracks += Track(
                    id = "local_$id",
                    title = cursor.getString(1) ?: "Unknown title",
                    artist = cursor.getString(2) ?: "Unknown artist",
                    durationSec = cursor.getLong(3) / 1000,
                    thumbnailUrl = ContentUris.withAppendedId(albumArtBaseUri, albumId).toString(),
                    source = TrackSource.LOCAL,
                    mediaUri = ContentUris
                        .withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                        .toString()
                )
            }
        }
        tracks
    }
}
