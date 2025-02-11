package com.blockstream.common.managers

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import com.blockstream.jade.Loggable

actual class LocaleManager(
    private val context: Context
) {

    actual fun getLocale(): String? {
        return LocaleManagerCompat.getApplicationLocales(context).toLanguageTags().takeIf { it.isNotBlank() }.also {
            logger.d { "Current locale is $it" }
        }
    }

    actual fun setLocale(locale: String?) {
        logger.d { "Setting locale to $locale" }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale))
    }

    companion object: Loggable()
}