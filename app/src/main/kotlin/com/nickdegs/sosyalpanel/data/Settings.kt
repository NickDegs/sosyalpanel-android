package com.nickdegs.sosyalpanel.data

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

// iOS @AppStorage("appLanguage") + .environment(\.locale) karşılığı.
// attachBaseContext senkron okuma gerektirdiği için SharedPreferences kullanır.
object LocaleHelper {
    private const val PREFS = "settings"
    private const val KEY = "app_language"

    // Çevirisi tam 14 dil (tr kaynak + 13). iOS ile birebir.
    val SUPPORTED = linkedMapOf(
        "" to "Sistem",
        "tr" to "Türkçe", "en" to "English", "es" to "Español", "fr" to "Français",
        "de" to "Deutsch", "it" to "Italiano", "pt" to "Português", "ru" to "Русский",
        "ar" to "العربية", "zh" to "中文", "hi" to "हिंदी", "uk" to "Українська",
        "az" to "Azərbaycanca", "kk" to "Қазақша"
    )

    fun current(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""

    fun persist(context: Context, code: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, code).apply()
    }

    // Seçilen dile göre context'i sarar. Boş → sistem dili (sarma yok).
    fun wrap(context: Context): Context {
        val code = current(context)
        if (code.isEmpty()) return context
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
