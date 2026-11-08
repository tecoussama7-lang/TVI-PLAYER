package store.oatec.playertv.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import store.oatec.playertv.model.MediaEntry

class PlayerPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("player_preferences", Context.MODE_PRIVATE)

    fun appLanguage(): String = preferences.getString(APP_LANGUAGE_KEY, "en") ?: "en"

    fun setAppLanguage(language: String) {
        val supported = setOf("ar", "fr", "en", "es", "de", "nl", "sv", "gsw", "it", "rm")
        if (language in supported) preferences.edit().putString(APP_LANGUAGE_KEY, language).apply()
    }

    fun favorites(): Set<String> = preferences.getString(FAVORITES_KEY, null)
        ?.let(::JSONArray)
        ?.let { array -> (0 until array.length()).mapNotNull(array::optString).toSet() }
        .orEmpty()

    fun setFavorite(id: String, favorite: Boolean) {
        val updated = favorites().toMutableSet().apply {
            if (favorite) add(id) else remove(id)
        }
        val array = JSONArray().apply { updated.forEach(::put) }
        preferences.edit().putString(FAVORITES_KEY, array.toString()).apply()
    }

    /** User-created channels are stored locally and kept separate from provider catalog data. */
    fun customChannels(): List<MediaEntry> = preferences.getString(CUSTOM_CHANNELS_KEY, null)
        ?.let(::JSONArray)
        ?.let { array ->
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val id = item.optString("id")
                val title = item.optString("title")
                val streamUrl = item.optString("streamUrl")
                if (id.isBlank() || title.isBlank() || streamUrl.isBlank()) null else MediaEntry(
                    id = id,
                    title = title,
                    category = item.optString("category", "Custom"),
                    streamUrl = streamUrl,
                    logoUrl = item.optString("logoUrl"),
                )
            }
        }.orEmpty()

    fun saveCustomChannel(channel: MediaEntry) {
        val channels = customChannels().filterNot { it.id == channel.id } + channel
        val array = JSONArray().apply {
            channels.forEach { item ->
                put(JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("category", item.category)
                    .put("streamUrl", item.streamUrl)
                    .put("logoUrl", item.logoUrl))
            }
        }
        preferences.edit().putString(CUSTOM_CHANNELS_KEY, array.toString()).apply()
    }

    fun removeCustomChannel(channelId: String) {
        val array = JSONArray().apply {
            customChannels().filterNot { it.id == channelId }.forEach { item ->
                put(JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("category", item.category)
                    .put("streamUrl", item.streamUrl)
                    .put("logoUrl", item.logoUrl))
            }
        }
        preferences.edit().putString(CUSTOM_CHANNELS_KEY, array.toString()).apply()
    }

    fun customCategories(): List<String> = preferences.getString(CATEGORIES_KEY, null)
        ?.let(::JSONArray)
        ?.let { array -> (0 until array.length()).map { array.optString(it) }.filter(String::isNotBlank) }
        .orEmpty()

    fun addCategory(name: String): Boolean {
        val normalized = name.trim()
        if (normalized.isBlank() || customCategories().any { it.equals(normalized, ignoreCase = true) }) return false
        val categories = customCategories() + normalized
        preferences.edit().putString(CATEGORIES_KEY, JSONArray(categories).toString()).apply()
        return true
    }

    fun categoryFor(channelId: String, sourceCategory: String): String =
        preferences.getString("category:$channelId", null) ?: sourceCategory

    fun moveToCategory(channelId: String, category: String) {
        preferences.edit().putString("category:$channelId", category).apply()
    }

    fun channelTitle(channelId: String, sourceTitle: String): String =
        preferences.getString("title:$channelId", null)?.takeIf(String::isNotBlank) ?: sourceTitle

    fun setChannelTitle(channelId: String, title: String) {
        preferences.edit().putString("title:$channelId", title.trim()).apply()
    }

    @Synchronized
    fun moveChannelToNumber(channelId: String, number: Int, knownChannelIds: List<String>): Boolean {
        if (number < 1 || channelId !in knownChannelIds) return false
        val ordered = knownChannelIds.distinct()
            .map { it to channelNumber(it) }
            .sortedBy { it.second }
            .map { it.first }
            .toMutableList()
        val currentIndex = ordered.indexOf(channelId)
        if (currentIndex < 0) return false
        ordered.removeAt(currentIndex)
        ordered.add((number - 1).coerceIn(0, ordered.size), channelId)

        val editor = preferences.edit()
        ordered.forEachIndexed { index, id ->
            editor.putInt("channel_number:$id", index + 1)
        }
        editor.putInt(NEXT_CHANNEL_NUMBER_KEY, ordered.size + 1).apply()
        return true
    }

    fun hiddenChannels(): Set<String> = preferences.getString(HIDDEN_KEY, null)
        ?.let(::JSONArray)
        ?.let { array -> (0 until array.length()).map { array.optString(it) }.toSet() }
        .orEmpty()

    fun setHidden(channelId: String, hidden: Boolean) {
        val ids = hiddenChannels().toMutableSet().apply {
            if (hidden) add(channelId) else remove(channelId)
        }
        preferences.edit().putString(HIDDEN_KEY, JSONArray(ids).toString()).apply()
    }

    fun progress(itemId: String): Long = preferences.getLong("progress:$itemId", 0L)

    fun duration(itemId: String): Long = preferences.getLong("duration:$itemId", 0L)

    fun saveProgress(itemId: String, positionMs: Long, durationMs: Long = 0L) {
        val position = positionMs.coerceAtLeast(0L)
        val duration = durationMs.coerceAtLeast(0L)
        val editor = preferences.edit()
        if (duration > 0L) editor.putLong("duration:$itemId", duration)
        if (duration > 0L && position >= duration - 30_000L) {
            editor.remove("progress:$itemId")
        } else if (position > 0L) {
            editor.putLong("progress:$itemId", position)
        } else {
            editor.remove("progress:$itemId")
        }
        editor.apply()
    }

    fun watchProgressFraction(itemId: String): Float {
        val position = progress(itemId)
        val duration = duration(itemId)
        if (position < 5_000L || duration <= 0L || position >= duration - 30_000L) return 0f
        return (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }

    fun resumePlayback(): Boolean = preferences.getBoolean(RESUME_PLAYBACK_KEY, true)

    fun setResumePlayback(enabled: Boolean) {
        val editor = preferences.edit().putBoolean(RESUME_PLAYBACK_KEY, enabled)
        if (!enabled) preferences.all.keys.filter { it.startsWith("progress:") }.forEach(editor::remove)
        editor.apply()
    }

    fun clearWatchHistory() {
        val editor = preferences.edit().remove(HISTORY_KEY)
        preferences.all.keys.filter { it.startsWith("progress:") }.forEach(editor::remove)
        editor.apply()
    }

    fun recordWatched(itemId: String) {
        val history = preferences.getString(HISTORY_KEY, null)
            ?.let(::JSONArray)
            ?.let { array -> (0 until array.length()).map { array.optString(it) } }
            .orEmpty()
        val updated = (listOf(itemId) + history.filterNot { it == itemId }).take(MAX_HISTORY_ITEMS)
        preferences.edit().putString(HISTORY_KEY, JSONArray(updated).toString()).apply()
    }

    fun recentlyWatched(entries: List<MediaEntry>): List<MediaEntry> {
        val byId = entries.associateBy { it.id }
        val history = preferences.getString(HISTORY_KEY, null)
            ?.let(::JSONArray)
            ?.let { array -> (0 until array.length()).map { array.optString(it) } }
            .orEmpty()
        return history.mapNotNull(byId::get).filter { progress(it.id) > MIN_RESUME_POSITION_MS }
    }

    /**
     * Keep the active lineup numbered 1..N while preserving its current user-defined order.
     * Removed playlist entries no longer leave gaps in the receiver channel numbers.
     */
    @Synchronized
    fun normalizeChannelNumbers(knownChannelIds: List<String>) {
        val ids = knownChannelIds.distinct()
        if (ids.isEmpty()) {
            preferences.edit().putInt(NEXT_CHANNEL_NUMBER_KEY, 1).apply()
            return
        }
        val ordered = ids.map { it to channelNumber(it) }.sortedBy { it.second }
        val editor = preferences.edit()
        ordered.forEachIndexed { index, (id, currentNumber) ->
            val sequentialNumber = index + 1
            if (currentNumber != sequentialNumber) {
                editor.putInt("channel_number:$id", sequentialNumber)
            }
        }
        editor.putInt(NEXT_CHANNEL_NUMBER_KEY, ids.size + 1).apply()
    }

    @Synchronized
    fun channelNumber(channelId: String): Int {
        val key = "channel_number:$channelId"
        val existing = preferences.getInt(key, 0)
        if (existing > 0) return existing
        val number = preferences.getInt(NEXT_CHANNEL_NUMBER_KEY, 1)
        preferences.edit()
            .putInt(key, number)
            .putInt(NEXT_CHANNEL_NUMBER_KEY, number + 1)
            .apply()
        return number
    }

    /** Kids Mode is device-local. The PIN is stored as a SHA-256 digest, never as plain text. */
    fun kidsModeEnabled(): Boolean = preferences.getBoolean(KIDS_MODE_ENABLED_KEY, false)

    fun setKidsModeEnabled(enabled: Boolean): Boolean {
        if (enabled && !hasKidsPin()) return false
        preferences.edit().putBoolean(KIDS_MODE_ENABLED_KEY, enabled).apply()
        return true
    }

    fun hasKidsPin(): Boolean = !preferences.getString(KIDS_PIN_HASH_KEY, null).isNullOrBlank()

    fun setKidsPin(pin: String): Boolean {
        val normalized = pin.trim()
        if (normalized.length !in 4..8 || normalized.any { !it.isDigit() }) return false
        preferences.edit()
            .putString(KIDS_PIN_HASH_KEY, pinDigest(normalized))
            .putBoolean(KIDS_MODE_ENABLED_KEY, true)
            .apply()
        return true
    }

    fun verifyKidsPin(pin: String): Boolean {
        val saved = preferences.getString(KIDS_PIN_HASH_KEY, null) ?: return false
        return java.security.MessageDigest.isEqual(
            saved.toByteArray(Charsets.UTF_8),
            pinDigest(pin.trim()).toByteArray(Charsets.UTF_8),
        )
    }

    fun changeKidsPin(currentPin: String, newPin: String): Boolean {
        if (!verifyKidsPin(currentPin)) return false
        val normalized = newPin.trim()
        if (normalized.length !in 4..8 || normalized.any { !it.isDigit() }) return false
        preferences.edit().putString(KIDS_PIN_HASH_KEY, pinDigest(normalized)).apply()
        return true
    }

    fun clearKidsPin(currentPin: String): Boolean {
        if (!verifyKidsPin(currentPin)) return false
        preferences.edit()
            .remove(KIDS_PIN_HASH_KEY)
            .putBoolean(KIDS_MODE_ENABLED_KEY, false)
            .apply()
        return true
    }

    /** Explicit parent decisions override automatic classification. */
    fun kidsAllowedItemIds(): Set<String> = readKidsIds(KIDS_ALLOW_ITEM_IDS_KEY)

    fun kidsBlockedItemIds(): Set<String> = readKidsIds(KIDS_BLOCK_ITEM_IDS_KEY)

    fun setKidsItemAllowed(itemId: String, allowed: Boolean) {
        val id = itemId.trim()
        if (id.isEmpty()) return
        val allow = kidsAllowedItemIds().toMutableSet()
        val block = kidsBlockedItemIds().toMutableSet()
        if (allowed) {
            allow.add(id)
            block.remove(id)
        } else {
            block.add(id)
            allow.remove(id)
        }
        preferences.edit()
            .putString(KIDS_ALLOW_ITEM_IDS_KEY, JSONArray(allow.toList()).toString())
            .putString(KIDS_BLOCK_ITEM_IDS_KEY, JSONArray(block.toList()).toString())
            .apply()
    }

    /** Remove a parent's explicit decision and return the item to automatic classification. */
    fun clearKidsItemRule(itemId: String): Boolean {
        val id = itemId.trim()
        if (id.isEmpty()) return false
        val allow = kidsAllowedItemIds().toMutableSet()
        val block = kidsBlockedItemIds().toMutableSet()
        val changed = allow.remove(id) or block.remove(id)
        if (changed) {
            preferences.edit()
                .putString(KIDS_ALLOW_ITEM_IDS_KEY, JSONArray(allow.toList()).toString())
                .putString(KIDS_BLOCK_ITEM_IDS_KEY, JSONArray(block.toList()).toString())
                .apply()
        }
        return changed
    }

    /** Unknown items are hidden while Kids Mode is active unless a parent allows them. */
    fun isAllowedInKidsMode(itemId: String, classifiedAsKidSafe: Boolean): Boolean {
        if (!kidsModeEnabled()) return true
        val id = itemId.trim()
        if (id.isEmpty() || id in kidsBlockedItemIds()) return false
        if (id in kidsAllowedItemIds()) return true
        return classifiedAsKidSafe
    }

    private fun readKidsIds(key: String): Set<String> = preferences.getString(key, null)
        ?.let(::JSONArray)
        ?.let { array -> (0 until array.length()).map { array.optString(it) }.filter(String::isNotBlank).toSet() }
        .orEmpty()

    private fun pinDigest(pin: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Selected playback engine: exoplayer (default) or mpv. */
    fun displayZoomMode(): String = preferences.getString(DISPLAY_ZOOM_MODE_KEY, "zoom")
        ?.lowercase()
        ?.takeIf { it in setOf("fit", "zoom", "full") } ?: "zoom"

    fun setDisplayZoomMode(mode: String) {
        val normalized = mode.trim().lowercase()
        if (normalized in setOf("fit", "zoom", "full")) {
            preferences.edit().putString(DISPLAY_ZOOM_MODE_KEY, normalized).apply()
        }
    }

    fun playbackEngine(): String = preferences.getString(PLAYBACK_ENGINE_KEY, "exoplayer")
        ?.lowercase()
        ?.takeIf { it in setOf("exoplayer", "mpv") } ?: "exoplayer"

    fun setPlaybackEngine(engine: String) {
        val normalized = engine.trim().lowercase()
        if (normalized in setOf("exoplayer", "mpv")) {
            preferences.edit().putString(PLAYBACK_ENGINE_KEY, normalized).apply()
        }
    }


    fun uiScale(): Float = preferences.getFloat(UI_SCALE_KEY, 1f).coerceIn(0.85f, 1.25f)
    fun setUiScale(value: Float) { preferences.edit().putFloat(UI_SCALE_KEY, value.coerceIn(0.85f, 1.25f)).apply() }

    fun highContrastEnabled(): Boolean = preferences.getBoolean(HIGH_CONTRAST_KEY, false)
    fun setHighContrastEnabled(enabled: Boolean) { preferences.edit().putBoolean(HIGH_CONTRAST_KEY, enabled).apply() }

    fun focusHighlightEnabled(): Boolean = preferences.getBoolean(FOCUS_HIGHLIGHT_KEY, true)
    fun setFocusHighlightEnabled(enabled: Boolean) { preferences.edit().putBoolean(FOCUS_HIGHLIGHT_KEY, enabled).apply() }

    fun autoReconnectEnabled(): Boolean = preferences.getBoolean(AUTO_RECONNECT_KEY, true)
    fun setAutoReconnectEnabled(enabled: Boolean) { preferences.edit().putBoolean(AUTO_RECONNECT_KEY, enabled).apply() }

    fun autoStartEnabled(): Boolean = preferences.getBoolean(AUTO_START_KEY, false)
    fun setAutoStartEnabled(enabled: Boolean) { preferences.edit().putBoolean(AUTO_START_KEY, enabled).apply() }

    fun sleepTimerDeadline(): Long = preferences.getLong(SLEEP_TIMER_DEADLINE_KEY, 0L)
    fun setSleepTimerDeadline(deadlineMillis: Long) { preferences.edit().putLong(SLEEP_TIMER_DEADLINE_KEY, deadlineMillis.coerceAtLeast(0L)).apply() }

    fun kidsScheduleEnabled(): Boolean = preferences.getBoolean(KIDS_SCHEDULE_ENABLED_KEY, false)
    fun kidsScheduleStart(): String = preferences.getString(KIDS_SCHEDULE_START_KEY, "08:00") ?: "08:00"
    fun kidsScheduleEnd(): String = preferences.getString(KIDS_SCHEDULE_END_KEY, "20:00") ?: "20:00"
    fun setKidsSchedule(enabled: Boolean, start: String, end: String): Boolean {
        val valid = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
        if (!valid.matches(start) || !valid.matches(end) || start == end) return false
        preferences.edit()
            .putBoolean(KIDS_SCHEDULE_ENABLED_KEY, enabled)
            .putString(KIDS_SCHEDULE_START_KEY, start)
            .putString(KIDS_SCHEDULE_END_KEY, end)
            .apply()
        return true
    }

    fun isWithinKidsSchedule(nowMinutes: Int = java.time.LocalTime.now().let { it.hour * 60 + it.minute }): Boolean {
        if (!kidsScheduleEnabled()) return true
        fun minutes(value: String): Int = value.substringBefore(":").toIntOrNull()?.times(60)
            ?.plus(value.substringAfter(":", "").toIntOrNull() ?: 0) ?: 0
        val start = minutes(kidsScheduleStart())
        val end = minutes(kidsScheduleEnd())
        return if (start < end) nowMinutes in start until end else nowMinutes >= start || nowMinutes < end
    }

    /** Export only this app's preferences; encrypted IPTV credentials live in a separate keystore-backed store. */
    fun exportSettingsJson(): String {
        val root = JSONObject().put("format", "oatec-player-settings").put("version", 1)
        val values = JSONObject()
        preferences.all.forEach { (key, value) ->
            if (key.startsWith("kids_")) return@forEach
            val item = JSONObject()
            when (value) {
                is String -> item.put("type", "string").put("value", value)
                is Boolean -> item.put("type", "boolean").put("value", value)
                is Int -> item.put("type", "int").put("value", value)
                is Long -> item.put("type", "long").put("value", value)
                is Float -> item.put("type", "float").put("value", value.toDouble())
                is Set<*> -> item.put("type", "string_set").put("value", JSONArray(value.filterIsInstance<String>()))
                else -> return@forEach
            }
            values.put(key, item)
        }
        return root.put("preferences", values).toString(2)
    }

    fun importSettingsJson(json: String): Boolean = runCatching {
        val root = JSONObject(json)
        require(root.optString("format") == "oatec-player-settings") { "This is not an OATEC settings backup." }
        val values = root.optJSONObject("preferences") ?: error("The backup has no settings.")
        val editor = preferences.edit()
        val keys = values.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val item = values.optJSONObject(key) ?: continue
            when (item.optString("type")) {
                "string" -> editor.putString(key, item.optString("value"))
                "boolean" -> editor.putBoolean(key, item.optBoolean("value"))
                "int" -> editor.putInt(key, item.optInt("value"))
                "long" -> editor.putLong(key, item.optLong("value"))
                "float" -> editor.putFloat(key, item.optDouble("value").toFloat())
                "string_set" -> {
                    val array = item.optJSONArray("value") ?: JSONArray()
                    editor.putStringSet(key, (0 until array.length()).map { array.optString(it) }.toSet())
                }
            }
        }
        editor.apply()
        true
    }.getOrDefault(false)

    /** Reset interface and convenience options without deleting channels, parental PIN, history or favourites. */
    fun resetInterfaceSettings() {
        preferences.edit()
            .remove(APP_LANGUAGE_KEY)
            .remove(RESUME_PLAYBACK_KEY)
            .remove(PLAYBACK_ENGINE_KEY)
            .remove(DISPLAY_ZOOM_MODE_KEY)
            .remove(UI_SCALE_KEY)
            .remove(HIGH_CONTRAST_KEY)
            .remove(FOCUS_HIGHLIGHT_KEY)
            .remove(AUTO_RECONNECT_KEY)
            .remove(AUTO_START_KEY)
            .remove(SLEEP_TIMER_DEADLINE_KEY)
            .remove(KIDS_SCHEDULE_ENABLED_KEY)
            .remove(KIDS_SCHEDULE_START_KEY)
            .remove(KIDS_SCHEDULE_END_KEY)
            .apply()
    }

    private companion object {

        const val UI_SCALE_KEY = "ui_scale"
        const val HIGH_CONTRAST_KEY = "high_contrast"
        const val FOCUS_HIGHLIGHT_KEY = "focus_highlight"
        const val AUTO_RECONNECT_KEY = "auto_reconnect"
        const val AUTO_START_KEY = "auto_start"
        const val SLEEP_TIMER_DEADLINE_KEY = "sleep_timer_deadline"
        const val KIDS_SCHEDULE_ENABLED_KEY = "kids_schedule_enabled"
        const val KIDS_SCHEDULE_START_KEY = "kids_schedule_start"
        const val KIDS_SCHEDULE_END_KEY = "kids_schedule_end"
        const val APP_LANGUAGE_KEY = "app_language"
        const val FAVORITES_KEY = "favorite_ids"
        const val KIDS_MODE_ENABLED_KEY = "kids_mode_enabled"
        const val KIDS_PIN_HASH_KEY = "kids_pin_sha256"
        const val KIDS_ALLOW_ITEM_IDS_KEY = "kids_allow_item_ids"
        const val KIDS_BLOCK_ITEM_IDS_KEY = "kids_block_item_ids"
        const val NEXT_CHANNEL_NUMBER_KEY = "next_channel_number"
        const val CATEGORIES_KEY = "custom_categories"
        const val CUSTOM_CHANNELS_KEY = "custom_channels"
        const val HIDDEN_KEY = "hidden_channels"
        const val HISTORY_KEY = "watch_history"
        const val RESUME_PLAYBACK_KEY = "resume_playback"
        const val PLAYBACK_ENGINE_KEY = "playback_engine"
        const val DISPLAY_ZOOM_MODE_KEY = "display_zoom_mode"
        const val MAX_HISTORY_ITEMS = 30
        const val MIN_RESUME_POSITION_MS = 60_000L
    }
}