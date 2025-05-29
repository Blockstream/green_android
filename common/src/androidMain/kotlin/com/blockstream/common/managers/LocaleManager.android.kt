package com.blockstream.common.managers

import android.content.Context
import android.telephony.TelephonyManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.os.LocaleListCompat
import com.blockstream.common.extensions.tryCatchNull
import com.blockstream.green.utils.Loggable
import java.util.Locale

actual class LocaleManager constructor(
    private val context: Context
) {

    actual fun getLocale(): String? {
        return LocaleManagerCompat.getApplicationLocales(context).toLanguageTags()
            .takeIf { it.isNotBlank() }.also {
                logger.d { "Current locale is $it" }
            }
    }

    actual fun setLocale(locale: String?) {
        logger.d { "Setting locale to $locale" }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale))
    }

    actual fun getCountry(): String? {
        return tryCatchNull {
            val tm = getSystemService(
                context,
                TelephonyManager::class.java
            )

            val fromNetwork = tm?.networkCountryIso
                ?.takeIf { it.isNotBlank() }

            val fromSim = tm?.simCountryIso
                ?.takeIf { it.isNotBlank() }

            val fromLocale = Locale.getDefault().country
                .takeIf { it.isNotBlank() }

            fromNetwork
                ?: fromSim
                ?: fromLocale
        }
    }

    companion object : Loggable()
}