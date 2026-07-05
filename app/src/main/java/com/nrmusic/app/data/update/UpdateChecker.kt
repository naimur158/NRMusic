package com.nrmusic.app.data.update

import com.nrmusic.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

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
 * Uses the plain github.com "/releases/latest" redirect (NOT api.github.com), because the
 * unauthenticated API is limited to 60 requests/hour per IP and returns HTTP 403 on shared
 * mobile networks. The redirect exposes the latest tag (e.g. .../releases/tag/v3); the CI
 * names each tag `v<versionCode>` and each APK `NRMusic-v<versionCode>.apk`, so both the
 * version and the download URL are derivable without the API.
 */
object UpdateChecker {

    private val client = OkHttpClient()

    val isConfigured: Boolean get() = BuildConfig.GITHUB_REPO.isNotBlank()

    suspend fun check(): UpdateResult = withContext(Dispatchers.IO) {
        val repo = BuildConfig.GITHUB_REPO
        if (repo.isBlank()) return@withContext UpdateResult.Failed("No update source configured")

        runCatching {
            val noRedirect = client.newBuilder().followRedirects(false).build()
            val req = Request.Builder()
                .url("https://github.com/$repo/releases/latest")
                .header("User-Agent", "NRMusic")
                .build()

            noRedirect.newCall(req).execute().use { resp ->
                // 302 → Location: https://github.com/owner/repo/releases/tag/v3
                // If there are no releases it redirects to .../releases (no "/tag/").
                val location = resp.header("Location")
                    ?: return@use UpdateResult.UpToDate
                val tag = location.substringAfterLast("/tag/", "")
                if (tag.isBlank()) return@use UpdateResult.UpToDate

                val remoteCode = tag.filter { it.isDigit() }.toIntOrNull()
                    ?: return@use UpdateResult.Failed("Unexpected release tag: $tag")

                if (remoteCode <= BuildConfig.VERSION_CODE) {
                    UpdateResult.UpToDate
                } else {
                    UpdateResult.Available(
                        versionName = "1.0.$remoteCode",
                        versionCode = remoteCode,
                        apkUrl = "https://github.com/$repo/releases/download/$tag/NRMusic-$tag.apk",
                        notes = ""
                    )
                }
            }
        }.getOrElse { UpdateResult.Failed(it.message ?: "Check failed") }
    }
}
