package com.blockstream.common.managers

val Locales = mapOf(
    null to null,
    "en" to "English",
    "cs" to "Čeština",
    "de" to "Deutsch",
    "es" to "Español",
    "fr" to "Français",
    "he" to "עברית",
    "it" to "Italiano",
    "ja" to "日本語",
    "ko" to "한국어",
    "nl" to "Nederlands",
    "pt" to "Português",
    "pt-BR" to "Português (Brasil)",
    "ro" to "Română",
    "ru" to "Русский",
    "uk" to "Українська",
    "vi" to "Tiếng Việt",
    "zh" to "中文"
)

expect class LocaleManager {
    fun getLocale(): String?
    fun setLocale(locale: String?)
}
