package com.nrmusic.app.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

sealed interface DownloadUpdateState {
    data class Progress(val fraction: Float) : DownloadUpdateState
    data object Ready : DownloadUpdateState
    data class Failed(val reason: String) : DownloadUpdateState
}

/** Downloads a release APK and launches the system installer. */
object UpdateManager {

    private val client = OkHttpClient()

    /**
     * Downloads [apkUrl] to the external cache, reporting progress via [onProgress],
     * then fires the install intent. Returns an error message on failure, or null on success.
     */
    suspend fun downloadAndInstall(
        context: Context,
        apkUrl: String,
        onProgress: (Float) -> Unit,
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.externalCacheDir, "updates").apply { mkdirs() }
            // Clear old APKs to avoid filling the cache.
            dir.listFiles()?.forEach { it.delete() }
            val apk = File(dir, "nrmusic-update.apk")

            client.newCall(Request.Builder().url(apkUrl).build()).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                val body = resp.body ?: throw IllegalStateException("Empty response")
                val total = body.contentLength()
                body.byteStream().use { input ->
                    apk.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = 0L
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) onProgress((downloaded.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            if (apk.length() == 0L) throw IllegalStateException("Downloaded 0 bytes")

            val uri: Uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", apk
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            null
        }.getOrElse { it.message ?: "Update failed" }
    }
}
