package com.nrmusic.app.data.update

import com.nrmusic.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

sealed interface UpdateResult {
    data object UpToDate : UpdateResult
    data class Available(
        val versionName: String,
        val versionCode: Int,
        val apkUrl: String,
        val notes: String,
    ) : UpdateResult
    data class Failed(val reason: String) : UpdateResult
}

/**
 * Checks the app's own GitHub repo for a newer release.
 *
 * The repo is injected at build time (BuildConfig.GITHUB_REPO); GitHub Actions passes it
 * automatically, so installed CI builds know where to look with zero configuration.
 * CI tags each release `v<versionCode>`, so we compare the tag number to our own versionCode.
 */
object UpdateChecker {

    private val client = OkHttpClient()

    val isConfigured: Boolean get() = BuildConfig.GITHUB_REPO.isNotBlank()

    suspend fun check(): UpdateResult = withContext(Dispatchers.IO) {
        val repo = BuildConfig.GITHUB_REPO
        if (repo.isBlank()) return@withContext UpdateResult.Failed("No update source configured")

        runCatching {
            val req = Request.Builder()
                .url("https://api.github.com/repos/$repo/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.code == 404) return@use UpdateResult.UpToDate // no releases yet
                if (!resp.isSuccessful) return@use UpdateResult.Failed("HTTP ${resp.code}")

                val json = JSONObject(resp.body?.string() ?: "{}")
                val tag = json.optString("tag_name")
                val remoteCode = tag.filter { it.isDigit() }.toIntOrNull()
                    ?: return@use UpdateResult.Failed("Bad release tag")
                val notes = json.optString("body").take(500)
                val versionName = json.optString("name").ifBlank { tag }

                val apkUrl = json.optJSONArray("assets")?.let { assets ->
                    (0 until assets.length())
                        .map { assets.getJSONObject(it) }
                        .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                        ?.optString("browser_download_url")
                }

                when {
                    remoteCode <= BuildConfig.VERSION_CODE -> UpdateResult.UpToDate
                    apkUrl.isNullOrBlank() -> UpdateResult.Failed("Release has no APK attached")
                    else -> UpdateResult.Available(versionName, remoteCode, apkUrl, notes)
                }
            }
        }.getOrElse { UpdateResult.Failed(it.message ?: "Check failed") }
    }
}
