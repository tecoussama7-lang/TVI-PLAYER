package store.oatec.playertv.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(
    val versionName: String,
    val releaseNotes: String,
    val releasePageUrl: String,
    val apkDownloadUrl: String,
)

class AppUpdateRepository {
    suspend fun checkForUpdate(context: Context): AppUpdate? = withContext(Dispatchers.IO) {
        val installedVersion = installedVersionName(context)
        val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "OATEC-PlayerTv-Android")
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) throw IOException("Update check failed (HTTP $status)")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val release = JSONObject(body)
            if (release.optBoolean("draft") || release.optBoolean("prerelease")) return@withContext null

            val tag = release.optString("tag_name").removePrefix("v").trim()
            if (tag.isBlank() || compareVersions(tag, installedVersion) <= 0) return@withContext null

            val assets = release.optJSONArray("assets") ?: return@withContext null
            var apkUrl = ""
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                val name = asset.optString("name")
                val url = asset.optString("browser_download_url")
                if (name.endsWith(".apk", ignoreCase = true) && url.startsWith("https://")) {
                    // Prefer a universal APK if one is ever published; otherwise use arm64.
                    if (name.contains("universal", ignoreCase = true)) {
                        apkUrl = url
                        break
                    }
                    if (apkUrl.isBlank() && name.contains("arm64", ignoreCase = true)) apkUrl = url
                }
            }
            if (apkUrl.isBlank()) return@withContext null
            AppUpdate(
                versionName = tag,
                releaseNotes = release.optString("body").take(4_000),
                releasePageUrl = release.optString("html_url"),
                apkDownloadUrl = apkUrl,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun installedVersionName(context: Context): String {
        val packageInfo = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return packageInfo.versionName.orEmpty()
    }

    private fun compareVersions(left: String, right: String): Int {
        fun parts(value: String) = value.substringBefore('-').split('.')
            .map { it.toIntOrNull() ?: 0 }
        val a = parts(left)
        val b = parts(right)
        for (index in 0 until maxOf(a.size, b.size)) {
            val comparison = (a.getOrElse(index) { 0 }).compareTo(b.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }

    companion object {
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/tecoussama7-lang/TVI-PLAYER/releases/latest"
    }
}
