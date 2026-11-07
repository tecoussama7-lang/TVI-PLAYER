package store.oatec.playertv.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import store.oatec.playertv.model.MediaEntry
import store.oatec.playertv.model.MediaKind
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

data class OfflineItem(
    val downloadId: Long,
    val title: String,
    val filePath: String,
    val logoUrl: String,
    val category: String,
    val url: String,
    val status: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
) {
    val isComplete: Boolean get() = status == DownloadManager.STATUS_SUCCESSFUL && File(filePath).exists()
    val progress: Int get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
    fun asMediaEntry() = MediaEntry(
        id = "offline:$downloadId",
        title = title,
        category = category,
        streamUrl = Uri.fromFile(File(filePath)).toString(),
        logoUrl = logoUrl,
        kind = MediaKind.MOVIE,
    )
}

/**
 * App-owned resumable downloader. Partial files are retained when paused and resumed with
 * HTTP Range requests when the origin supports them. A server that ignores Range restarts safely.
 */
class OfflineDownloadStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("offline_downloads", Context.MODE_PRIVATE)
    private val executor = Executors.newCachedThreadPool()
    private val running = ConcurrentHashMap.newKeySet<Long>()
    private val paused = ConcurrentHashMap.newKeySet<Long>()

    @Synchronized
    fun enqueue(entry: MediaEntry): Long {
        val uri = Uri.parse(entry.streamUrl.trim())
        require(uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()) {
            "هاد المصدر ما عندوش رابط تحميل مباشر صالح."
        }
        require(!uri.path.orEmpty().endsWith(".m3u8", true) && !uri.path.orEmpty().endsWith(".mpd", true)) {
            "هاد الفيديو بث HLS/DASH؛ التحميل Offline ديالو مازال غير مدعوم."
        }
        require(entry.kind != MediaKind.LIVE && entry.kind != MediaKind.SERIES) {
            "اختار فيلم أو حلقة، ماشي قناة مباشرة أو صفحة مسلسل."
        }
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: context.filesDir.resolve("offline")
        if (!dir.exists()) dir.mkdirs()
        val extension = uri.path.orEmpty().substringAfterLast('.', "mp4")
            .takeIf { it.matches(Regex("[A-Za-z0-9]{2,5}")) } ?: "mp4"
        val safeTitle = entry.title.replace(Regex("[^A-Za-z0-9._-]"), "_").take(55).ifBlank { "video" }
        val id = System.currentTimeMillis()
        val fileName = "${safeTitle}_${id}.$extension"
        val item = JSONObject()
            .put("id", id)
            .put("title", entry.title)
            .put("file", fileName)
            .put("logo", entry.logoUrl)
            .put("category", entry.category)
            .put("url", entry.streamUrl)
            .put("status", DownloadManager.STATUS_PENDING)
            .put("downloaded", 0L)
            .put("total", -1L)
        val items = readMetadata()
        items.put(item)
        save(items)
        start(id)
        return id
    }

    @Synchronized
    fun list(): List<OfflineItem> {
        val metadata = readMetadata()
        val results = mutableListOf<OfflineItem>()
        val dir = directory()
        for (i in 0 until metadata.length()) {
            val obj = metadata.optJSONObject(i) ?: continue
            val id = obj.optLong("id", -1L)
            if (id < 0) continue
            val file = File(dir, obj.optString("file"))
            val status = obj.optInt("status", if (file.exists() && file.length() > 0) DownloadManager.STATUS_SUCCESSFUL else DownloadManager.STATUS_FAILED)
            val bytes = if (file.exists()) file.length() else obj.optLong("downloaded", 0L)
            results += OfflineItem(
                downloadId = id,
                title = obj.optString("title", "Video"),
                filePath = file.absolutePath,
                logoUrl = obj.optString("logo"),
                category = obj.optString("category"),
                url = obj.optString("url"),
                status = status,
                downloadedBytes = bytes,
                totalBytes = obj.optLong("total", -1L),
            )
            if (status == DownloadManager.STATUS_PENDING || status == DownloadManager.STATUS_RUNNING) {
                start(id)
            }
        }
        return results.sortedByDescending { it.downloadId }
    }

    fun pause(downloadId: Long) {
        paused.add(downloadId)
        update(downloadId) { it.put("status", DownloadManager.STATUS_PAUSED) }
    }

    fun resume(downloadId: Long) {
        paused.remove(downloadId)
        update(downloadId) { it.put("status", DownloadManager.STATUS_PENDING) }
        start(downloadId)
    }

    fun retry(downloadId: Long) {
        paused.remove(downloadId)
        update(downloadId) { it.put("status", DownloadManager.STATUS_PENDING) }
        start(downloadId)
    }

    @Synchronized
    fun delete(downloadId: Long) {
        paused.add(downloadId)
        val array = readMetadata()
        val remaining = JSONArray()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            if (obj.optLong("id") == downloadId) {
                File(directory(), obj.optString("file")).delete()
            } else remaining.put(obj)
        }
        save(remaining)
    }

    private fun start(id: Long) {
        if (paused.contains(id) || !running.add(id)) return
        executor.execute {
            try {
                transfer(id)
            } finally {
                running.remove(id)
            }
        }
    }

    private fun transfer(id: Long) {
        val item = find(id) ?: return
        val target = File(directory(), item.optString("file"))
        val parent = target.parentFile ?: return
        if (!parent.exists()) parent.mkdirs()
        val existing = if (target.exists()) target.length() else 0L
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(item.optString("url")).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20000
                readTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "TVI-PLAYER/1.0")
                if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
            }
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            val append = existing > 0 && code == HttpURLConnection.HTTP_PARTIAL
            val total = if (append) {
                val range = connection.getHeaderField("Content-Range")
                range?.substringAfterLast('/')?.toLongOrNull() ?: (existing + connection.contentLengthLong)
            } else connection.contentLengthLong
            if (!append && existing > 0) target.delete()
            update(id) {
                it.put("status", DownloadManager.STATUS_RUNNING)
                it.put("total", total)
                it.put("downloaded", if (append) existing else 0L)
            }
            connection.inputStream.use { input ->
                RandomAccessFile(target, "rw").use { output ->
                    if (append) output.seek(existing) else output.setLength(0)
                    val buffer = ByteArray(64 * 1024)
                    var lastSave = System.currentTimeMillis()
                    while (true) {
                        if (paused.contains(id)) {
                            update(id) {
                                it.put("status", DownloadManager.STATUS_PAUSED)
                                it.put("downloaded", output.length())
                            }
                            return
                        }
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        val now = System.currentTimeMillis()
                        if (now - lastSave >= 500) {
                            update(id) { it.put("downloaded", output.length()) }
                            lastSave = now
                        }
                    }
                    update(id) { it.put("downloaded", output.length()) }
                }
            }
            update(id) {
                it.put("status", DownloadManager.STATUS_SUCCESSFUL)
                it.put("downloaded", target.length())
            }
        } catch (error: Exception) {
            if (paused.contains(id)) {
                update(id) { it.put("status", DownloadManager.STATUS_PAUSED); it.put("downloaded", target.length()) }
            } else {
                update(id) {
                    it.put("status", DownloadManager.STATUS_FAILED)
                    it.put("downloaded", target.length())
                    it.put("error", error.message ?: "Download failed")
                }
            }
        } finally {
            connection?.disconnect()
        }
    }

    private fun directory(): File =
        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir.resolve("offline")

    @Synchronized
    private fun find(id: Long): JSONObject? {
        val array = readMetadata()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            if (obj.optLong("id") == id) return JSONObject(obj.toString())
        }
        return null
    }

    @Synchronized
    private fun update(id: Long, change: (JSONObject) -> Unit) {
        val array = readMetadata()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            if (obj.optLong("id") == id) {
                change(obj)
                break
            }
        }
        save(array)
    }

    private fun save(array: JSONArray) {
        prefs.edit().putString("items", array.toString()).apply()
    }

    private fun readMetadata(): JSONArray = runCatching {
        JSONArray(prefs.getString("items", "[]") ?: "[]")
    }.getOrDefault(JSONArray())
}
