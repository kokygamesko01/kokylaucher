package com.mclauncher.android.util

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

const val PREFS_NAME = "kokylaucher_prefs"
const val KEY_SELECTED_LANGUAGE = "selected_language"

/**
 * Supported languages with their native names and locale codes.
 */
object SupportedLanguages {
    val all = listOf(
        Language("en", "English", Locale.ENGLISH),
        Language("cs", "Čeština", Locale("cs")),
        Language("ja", "日本語", Locale("ja")),
        Language("sk", "Slovenčina", Locale("sk")),
        Language("de", "Deutsch", Locale("de")),
        Language("fr", "Français", Locale("fr")),
        Language("es", "Español", Locale("es")),
        Language("pt", "Português", Locale("pt")),
        Language("pl", "Polski", Locale("pl")),
        Language("zh-rCN", "简体中文", Locale("zh", "CN"))
    )

    fun getByCode(code: String): Language? = all.find { it.code == code }
    fun getDefault(): Language = all[0] // English
}

data class Language(val code: String, val name: String, val locale: Locale)

/**
 * Helper for managing app-level locale independent of system locale.
 */
object LocaleHelper {

    /**
     * Get the currently selected language from SharedPreferences, or default to English.
     */
    fun getSelectedLanguage(context: Context): Language {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_SELECTED_LANGUAGE, null)
        return SupportedLanguages.getByCode(code ?: "") ?: SupportedLanguages.getDefault()
    }

    /**
     * Save the selected language to SharedPreferences.
     */
    fun saveSelectedLanguage(context: Context, language: Language) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_LANGUAGE, language.code).apply()
    }

    /**
     * Create a context wrapper with the selected locale applied.
     * Call this in Activity.attachBaseContext() before super.attachBaseContext().
     */
    fun wrapContext(context: Context): Context {
        val language = getSelectedLanguage(context)
        val locale = language.locale

        val resources = context.resources
        val configuration = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            configuration.setLocales(localeList)
            return context.createConfigurationContext(configuration)
        } else {
            configuration.locale = locale
            Resources.getSystem().updateConfiguration(configuration, resources.displayMetrics)
            return ContextWrapper(context)
        }
    }
}
