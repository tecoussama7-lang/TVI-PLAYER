package store.oatec.playertv.ui

/** Supported interface languages. Provider-supplied media metadata is intentionally not rewritten. */
internal data class AppLanguage(val code: String, val nativeName: String, val localeTag: String) {
    companion object {
        val supported = listOf(
            AppLanguage("ar", "العربية", "ar-MA"),
            AppLanguage("fr", "Français", "fr-FR"),
            AppLanguage("en", "English", "en"),
            AppLanguage("es", "Español", "es-ES"),
            AppLanguage("de", "Deutsch", "de-DE"),
            AppLanguage("nl", "Nederlands", "nl-NL"),
            AppLanguage("sv", "Svenska", "sv-SE"),
            AppLanguage("gsw", "Schweizerdeutsch", "gsw-CH"),
            AppLanguage("it", "Italiano", "it-IT"),
            AppLanguage("rm", "Rumantsch", "rm-CH"),
        )
    }
}
