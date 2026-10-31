package store.oatec.playertv.data

import android.content.Context
import android.os.Build
import java.util.UUID

/**
 * Client-side model for the future OATEC licensing API.
 *
 * IMPORTANT: the local trial timestamp is a UX fallback only. It is not tamper-proof.
 * Production access decisions must come from the OATEC server and be signed/validated.
 */
data class TrialStatus(
    val startedAtMillis: Long,
    val expiresAtMillis: Long,
    val remainingMillis: Long,
) {
    val isActive: Boolean get() = remainingMillis > 0L
}

class AppEntitlementRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Starts the 7-day trial exactly once on this installation.
     * Server-side first-seen/device records must replace this local-only source of truth
     * before public commercial release.
     */
    fun getOrStartTrial(nowMillis: Long = System.currentTimeMillis()): TrialStatus {
        val startedAt = prefs.getLong(KEY_TRIAL_STARTED_AT, 0L).let { saved ->
            if (saved > 0L) saved else nowMillis.also {
                prefs.edit().putLong(KEY_TRIAL_STARTED_AT, it).apply()
            }
        }
        val expiresAt = startedAt + TRIAL_DURATION_MILLIS
        return TrialStatus(
            startedAtMillis = startedAt,
            expiresAtMillis = expiresAt,
            remainingMillis = (expiresAt - nowMillis).coerceAtLeast(0L),
        )
    }

    /** Stable per-installation identifier, shared with the existing Premium activation flow. */
    fun deviceId(): String {
        val activationPrefs = appContext.getSharedPreferences("oatec_activation", Context.MODE_PRIVATE)
        return activationPrefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            activationPrefs.edit().putString("device_id", it).apply()
        }
    }

    fun activationDevicePayload(): Map<String, String> = mapOf(
        "device_id" to deviceId(),
        "device_name" to (Build.MANUFACTURER + " " + Build.MODEL).trim().take(255),
        "platform" to "android",
    )

    companion object {
        const val TRIAL_DURATION_MILLIS = 7L * 24L * 60L * 60L * 1000L
        const val API_BASE_URL = "https://admin.oatec.store/api/player"
        const val ENTITLEMENT_STATUS_PATH = "/entitlement/status"
        const val ENTITLEMENT_ACTIVATE_PATH = "/entitlement/activate"
        private const val PREFS_NAME = "oatec_app_entitlement"
        private const val KEY_TRIAL_STARTED_AT = "trial_started_at"
    }
}
