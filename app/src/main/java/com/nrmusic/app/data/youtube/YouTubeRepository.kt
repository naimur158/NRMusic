package com.nrmusic.app.data.youtube

import com.nrmusic.app.data.model.Track
import com.nrmusic.app.data.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException

/** A YouTube Music album/single search result. */
data class AlbumResult(val name: String, val uploader: String, val thumbnail: String?, val url: String)

/** A YouTube Music artist/channel search result. */
data class ArtistResult(val name: String, val thumbnail: String?, val url: String)

object YouTubeRepository {

    private val service = ServiceList.YouTube

    /** Searches YouTube Music songs. */
    suspend fun searchSongs(query: String): List<Track> = withContext(Dispatchers.IO) {
        searchSongsInternal(query)
    }

    private fun searchSongsInternal(query: String): List<Track> {
        val handler = service.searchQHFactory.fromQuery(
            query,
            listOf(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS),
            ""
        )
        val extractor = service.getSearchExtractor(handler)
        extractor.fetchPage()
        return extractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .map { it.toTrack() }
    }

    /**
     * Music-only "trending" for the Home screen. The general trending kiosk mixes in
     * sports/news/livestreams, so instead we pull actual songs via the music-songs
     * search filter for a rotating popular query.
     */
    suspend fun trendingMusic(): List<Track> = withContext(Dispatchers.IO) {
        val query = trendingQueries.random()
        searchSongsInternal(query)
    }

    private val trendingQueries = listOf(
        "top hits 2026",
        "trending songs this week",
        "popular music right now",
        "global top 50 songs",
        "new music this week"
    )

    /** Search filtered to plain videos (not just music songs). */
    suspend fun searchVideos(query: String): List<Track> = withContext(Dispatchers.IO) {
        val handler = service.searchQHFactory.fromQuery(
            query, listOf(YoutubeSearchQueryHandlerFactory.VIDEOS), ""
        )
        val extractor = service.getSearchExtractor(handler)
        extractor.fetchPage()
        extractor.initialPage.items.filterIsInstance<StreamInfoItem>().map { it.toTrack() }
    }

    /** Search YouTube Music albums. */
    suspend fun searchAlbums(query: String): List<AlbumResult> = withContext(Dispatchers.IO) {
        val handler = service.searchQHFactory.fromQuery(
            query, listOf(YoutubeSearchQueryHandlerFactory.MUSIC_ALBUMS), ""
        )
        val extractor = service.getSearchExtractor(handler)
        extractor.fetchPage()
        extractor.initialPage.items.filterIsInstance<PlaylistInfoItem>().map {
            AlbumResult(
                name = it.name ?: "Unknown album",
                uploader = it.uploaderName ?: "",
                thumbnail = it.thumbnails.maxByOrNull { t -> t.width }?.url,
                url = it.url
            )
        }
    }

    /** Search YouTube Music artists. */
    suspend fun searchArtists(query: String): List<ArtistResult> = withContext(Dispatchers.IO) {
        val handler = service.searchQHFactory.fromQuery(
            query, listOf(YoutubeSearchQueryHandlerFactory.MUSIC_ARTISTS), ""
        )
        val extractor = service.getSearchExtractor(handler)
        extractor.fetchPage()
        extractor.initialPage.items.filterIsInstance<ChannelInfoItem>().map {
            ArtistResult(
                name = it.name ?: "Unknown artist",
                thumbnail = it.thumbnails.maxByOrNull { t -> t.width }?.url,
                url = it.url
            )
        }
    }

    /** All songs by an artist (best effort — a music-songs search for the name). */
    suspend fun artistSongs(name: String): List<Track> = withContext(Dispatchers.IO) {
        searchSongsInternal(name)
    }

    /** Tracks of an album/playlist by its URL. */
    suspend fun albumTracks(url: String): List<Track> = withContext(Dispatchers.IO) {
        val info = PlaylistInfo.getInfo(service, url)
        info.relatedItems.filterIsInstance<StreamInfoItem>().map { it.toTrack() }
    }

    /** "Because you liked" — related songs to a seed video. */
    suspend fun relatedTo(videoId: String): List<Track> = withContext(Dispatchers.IO) {
        val info = StreamInfo.getInfo(service, "https://www.youtube.com/watch?v=$videoId")
        info.relatedItems.filterIsInstance<StreamInfoItem>().map { it.toTrack() }
    }

    /** Search-as-you-type suggestions. */
    suspend fun suggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        runCatching { service.suggestionExtractor.suggestionList(query) }.getOrDefault(emptyList())
    }

    /**
     * Resolves the direct audio stream URL for a video id.
     * Blocking on purpose: called from ExoPlayer's loading thread via ResolvingDataSource.
     */
    fun resolveStreamUrlBlocking(videoId: String): String {
        val info = StreamInfo.getInfo(service, "https://www.youtube.com/watch?v=$videoId")
        val audio = info.audioStreams.maxByOrNull { it.averageBitrate }
            ?: throw IOException("No audio stream found for $videoId")
        return audio.content
    }

    private fun StreamInfoItem.toTrack(): Track {
        val videoId = url.substringAfter("v=").substringBefore("&")
        return Track(
            id = videoId,
            title = name ?: "Unknown title",
            artist = uploaderName ?: "Unknown artist",
            durationSec = duration,
            thumbnailUrl = thumbnails.maxByOrNull { it.width }?.url ?: thumbnails.lastOrNull()?.url,
            source = TrackSource.YOUTUBE,
            mediaUri = "nrmusic://youtube/$videoId"
        )
    }
}
