package store.oatec.playertv.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TrialStatus(
    val startedAtMillis: Long,
    val expiresAtMillis: Long,
    val remainingMillis: Long,
) {
    val isActive: Boolean get() = remainingMillis > 0L
}

data class ServerTrialStatus(
    val active: Boolean,
    val startedAt: String?,
    val expiresAt: String?,
    val remainingSeconds: Long,
    val serverTime: String?,
)

data class StandardDeviceIdentity(
    val id: String,
    val key: String,
)

class AppEntitlementRepository(context: Context) {
    private val appContext = context.applicationContext

    /** Creates the Standard trial once on the server, then always returns the server record. */
    suspend fun startOrGetStandardTrial(): ServerTrialStatus = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("device_id", standardTrialDeviceId())
            .put("device_name", (Build.MANUFACTURER + " " + Build.MODEL).trim().take(255))
            .put("platform", "android")
            .put("app_version", "1.0.0")
        val response = requestJson(
            method = "POST",
            path = "/entitlement/standard-trial",
            body = payload.toString(),
        )
        parseStandardStatus(response)
    }

    /** Retrieves the stable website credentials for this Standard Player device. */
    suspend fun getStandardDeviceIdentity(): StandardDeviceIdentity = withContext(Dispatchers.IO) {
        val response = requestJson(
            method = "POST",
            path = "/standard/identity",
            body = JSONObject().put("device_id", standardTrialDeviceId()).toString(),
        )
        if (response.optString("mode") != "standard") {
            throw IllegalStateException("Unexpected device identity mode returned by the server.")
        }
        val id = response.optString("id").trim()
        val key = response.optString("key").trim()
        if (id.isBlank() || key.isBlank()) {
            throw IllegalStateException("The server did not return the Standard device credentials.")
        }
        StandardDeviceIdentity(id = id, key = key)
    }

    /** Reads the authoritative Standard entitlement without creating or extending a trial. */
    suspend fun getStandardTrialStatus(): ServerTrialStatus = withContext(Dispatchers.IO) {
        val encodedId = java.net.URLEncoder.encode(standardTrialDeviceId(), "UTF-8")
        val response = requestJson(
            method = "GET",
            path = "/entitlement/status?device_id=$encodedId",
            body = null,
        )
        val standard = response.optJSONObject("standard")
            ?: throw IllegalStateException("The server did not return Standard trial status.")
        ServerTrialStatus(
            active = standard.optBoolean("active", false),
            startedAt = standard.optString("started_at").takeIf { it.isNotBlank() && it != "null" },
            expiresAt = standard.optString("expires_at").takeIf { it.isNotBlank() && it != "null" },
            remainingSeconds = standard.optLong("remaining_seconds", 0L).coerceAtLeast(0L),
            serverTime = response.optString("server_time").takeIf { it.isNotBlank() },
        )
    }

    private fun parseStandardStatus(response: JSONObject): ServerTrialStatus {
        if (response.optString("mode") != "standard") {
            throw IllegalStateException("Unexpected entitlement mode returned by the server.")
        }
        return ServerTrialStatus(
            active = response.optBoolean("active", false),
            startedAt = response.optString("started_at").takeIf { it.isNotBlank() && it != "null" },
            expiresAt = response.optString("expires_at").takeIf { it.isNotBlank() && it != "null" },
            remainingSeconds = response.optLong("remaining_seconds", 0L).coerceAtLeast(0L),
            serverTime = response.optString("server_time").takeIf { it.isNotBlank() },
        )
    }

    private fun requestJson(method: String, path: String, body: String?): JSONObject {
        val connection = (URL(API_BASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12000
            readTimeout = 15000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-store")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrElse {
                throw IllegalStateException("The licensing server returned an unreadable response.")
            }
            if (status !in 200..299 || !json.optBoolean("ok", false)) {
                val reason = json.optString("error").ifBlank { "HTTP $status" }
                throw IllegalStateException(reason)
            }
            return json
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Stable identifier for Standard licensing. Android ID survives clearing this app's data,
     * unlike the app-local UUID used by the existing Premium activation flow.
     */
    private fun standardTrialDeviceId(): String {
        val androidId = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        if (androidId.isNotBlank()) {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest((appContext.packageName + ":" + androidId).toByteArray(Charsets.UTF_8))
                .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
            return "std-$digest"
        }
        val prefs = appContext.getSharedPreferences("oatec_standard_trial", Context.MODE_PRIVATE)
        return prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("device_id", it).apply()
        }
    }

    /** Stable per-installation identifier shared with the existing activation flow. */
    fun deviceId(): String {
        val activationPrefs = appContext.getSharedPreferences("oatec_activation", Context.MODE_PRIVATE)
        return activationPrefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            activationPrefs.edit().putString("device_id", it).apply()
        }
    }

    companion object {
        const val TRIAL_DURATION_MILLIS = 7L * 24L * 60L * 60L * 1000L
        const val API_BASE_URL = "https://admin.oatec.store/api/player"
    }
}
