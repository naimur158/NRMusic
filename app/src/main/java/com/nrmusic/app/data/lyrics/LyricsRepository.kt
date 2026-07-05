package com.nrmusic.app.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

data class LyricLine(val timeMs: Long, val text: String)

data class Lyrics(
    val synced: List<LyricLine>,
    val plain: String?,
) {
    val hasSynced: Boolean get() = synced.isNotEmpty()
    val isEmpty: Boolean get() = synced.isEmpty() && plain.isNullOrBlank()
}

/**
 * Fetches lyrics from lrclib.net — a free, open, no-key lyrics API.
 * Returns synced (LRC) lines when available, otherwise plain text.
 */
object LyricsRepository {

    private val client = OkHttpClient()
    private val cache = HashMap<String, Lyrics>()

    suspend fun fetch(title: String, artist: String, durationSec: Long): Lyrics =
        withContext(Dispatchers.IO) {
            val key = "$title|$artist"
            cache[key]?.let { return@withContext it }

            val url = buildString {
                append("https://lrclib.net/api/get")
                append("?track_name=").append(enc(title))
                append("&artist_name=").append(enc(artist))
                if (durationSec > 0) append("&duration=").append(durationSec)
            }
            val result = runCatching {
                val req = Request.Builder().url(url)
                    .header("User-Agent", "NRMusic (personal use)")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use Lyrics(emptyList(), null)
                    val body = resp.body?.string() ?: return@use Lyrics(emptyList(), null)
                    parse(body)
                }
            }.getOrDefault(Lyrics(emptyList(), null))

            cache[key] = result
            result
        }

    private fun parse(json: String): Lyrics {
        val obj = JSONObject(json)
        val plain = obj.optString("plainLyrics").takeIf { it.isNotBlank() }
        val syncedRaw = obj.optString("syncedLyrics")
        val synced = if (syncedRaw.isNotBlank()) parseLrc(syncedRaw) else emptyList()
        return Lyrics(synced, plain)
    }

    private val lrcPattern = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")

    private fun parseLrc(raw: String): List<LyricLine> {
        val lines = ArrayList<LyricLine>()
        raw.lineSequence().forEach { line ->
            val matches = lrcPattern.findAll(line).toList()
            if (matches.isEmpty()) return@forEach
            val text = line.substring(matches.last().range.last + 1).trim()
            for (m in matches) {
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val ms = when (frac.length) {
                    3 -> frac.toLong()
                    2 -> frac.toLong() * 10
                    1 -> frac.toLong() * 100
                    else -> 0L
                }
                lines.add(LyricLine(min * 60_000 + sec * 1000 + ms, text))
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}
