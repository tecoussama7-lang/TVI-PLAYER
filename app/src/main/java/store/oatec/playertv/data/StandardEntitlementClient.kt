package store.oatec.playertv.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

data class StandardEntitlement(
    val status: String,
    val trialExpiresAt: String = "",
    val paymentUrl: String = "",
)

class StandardEntitlementClient(context: Context) {
    private val app = context.applicationContext

    fun deviceId(): String {
        val prefs = app.getSharedPreferences("oatec_activation", Context.MODE_PRIVATE)
        return prefs.getString("device_id", null) ?: run {
            val androidId = Settings.Secure.getString(app.contentResolver, Settings.Secure.ANDROID_ID)
                ?.takeIf { it.isNotBlank() && it != "9774d56d682e549c" }
            val stableId = androidId?.let { "android-$it" } ?: UUID.randomUUID().toString()
            prefs.edit().putString("device_id", stableId).apply()
            stableId
        }
    }

    fun register(): StandardEntitlement = request(true)
    fun status(): StandardEntitlement = request(false)

    fun submitPayment(reference: String, contact: String = "") {
        val payload = JSONObject()
            .put("device_id", deviceId())
            .put("payment_reference", reference.trim())
            .put("payment_method", "Streamshop")
            .put("contact", contact.trim())
        val connection = open("/standard/payment-request", "POST")
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            readResponse(connection)
        } finally { connection.disconnect() }
    }

    private fun request(register: Boolean): StandardEntitlement {
        val id = deviceId()
        val endpoint = if (register) "/standard/register" else "/standard/status?device_id=" + URLEncoder.encode(id, "UTF-8")
        val connection = open(endpoint, if (register) "POST" else "GET")
        try {
            if (register) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                val payload = JSONObject()
                    .put("device_id", id)
                    .put("device_name", (Build.MANUFACTURER + " " + Build.MODEL).trim().take(255))
                    .put("platform", "android")
                    .put("app_version", "1.0.0")
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            }
            val json = readResponse(connection)
            return StandardEntitlement(
                status = json.optString("status", "expired"),
                trialExpiresAt = json.optString("trial_expires_at"),
                paymentUrl = json.optString("payment_url"),
            )
        } finally { connection.disconnect() }
    }

    private fun open(path: String, method: String): HttpURLConnection =
        (URL(API_BASE + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
        }

    private fun readResponse(connection: HttpURLConnection): JSONObject {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        val json = runCatching { JSONObject(body) }.getOrNull()
        if (code !in 200..299 || json?.optBoolean("ok") != true) {
            throw IOException(json?.optString("error").orEmpty().ifBlank { "تعذر التحقق من ترخيص Standard (HTTP $code)." })
        }
        return json
    }

    companion object {
        private const val API_BASE = "https://admin.oatec.store/api/player"
    }
}
