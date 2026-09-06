package com.nothingisland.app.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.nothingisland.app.model.AppUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class GitHubUpdateManager(
    private val context: Context,
    private val repoOwner: String = "imadraude",
    private val repoName: String = "NothingIsland"
) {

    suspend fun checkForUpdate(currentVersionName: String): Result<AppUpdate> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "NothingIsland-Android-App")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(
                    IllegalStateException("HTTP ${connection.responseCode} while checking updates")
                )
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseBody)
            val latestTagName = json.optString("tag_name", "")
            val releaseNotes = json.optString("body", "Нова версія доступна для встановлення.")
            val assetsArray = json.optJSONArray("assets")

            var apkUrl = ""
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            val cleanLatest = latestTagName.removePrefix("v").trim()
            val cleanCurrent = currentVersionName.removePrefix("v").trim()
            val hasUpdate = isNewerVersion(cleanCurrent, cleanLatest) && apkUrl.isNotEmpty()

            Result.success(
                AppUpdate(
                    latestVersionName = latestTagName,
                    releaseNotes = releaseNotes,
                    apkDownloadUrl = apkUrl,
                    isUpdateAvailable = hasUpdate
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, "NothingIsland_update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            var currentUrl = downloadUrl
            var connection: HttpURLConnection
            var redirects = 0
            val maxRedirects = 5

            while (true) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "NothingIsland-Android-App")
                    connectTimeout = 15000
                    readTimeout = 30000
                }

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_SEE_OTHER
                ) {
                    currentUrl = connection.getHeaderField("Location")
                        ?: throw IllegalStateException("Redirect URL missing")
                    redirects++
                    if (redirects > maxRedirects) {
                        throw IllegalStateException("Too many redirects")
                    }
                    continue
                }

                if (status != HttpURLConnection.HTTP_OK) {
                    throw IllegalStateException("Download failed: HTTP $status")
                }
                break
            }

            val fileLength = connection.contentLength
            val inputStream: InputStream = connection.inputStream
            val outputStream = FileOutputStream(apkFile)

            val buffer = ByteArray(8 * 1024)
            var totalBytesRead: Long = 0
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (fileLength > 0) {
                    val progress = ((totalBytesRead * 100) / fileLength).toInt()
                    onProgress(progress.coerceIn(0, 100))
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            Result.success(apkFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun installApk(apkFile: File): Result<Unit> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val permissionIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(permissionIntent)
                    return Result.failure(
                        SecurityException("Надайте дозвіл на встановлення невідомих додатків для Nothing Island.")
                    )
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun isNewerVersion(current: String, latest: String): Boolean {
            val currentParts = current.removePrefix("v").split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
            val latestParts = latest.removePrefix("v").split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
            val maxLength = maxOf(currentParts.size, latestParts.size)

            for (i in 0 until maxLength) {
                val c = currentParts.getOrElse(i) { 0 }
                val l = latestParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            return false
        }
    }
}
