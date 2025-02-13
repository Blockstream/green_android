package com.blockstream.common.managers

import androidx.compose.ui.text.intl.Locale
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual class LocaleManager {

    actual fun getLocale(): String? {
        return NSLocale.currentLocale.languageCode
    }

    actual fun setLocale(locale: String?) {
        // TODO
    }

    actual fun getCountry() : String? = Locale.current.region
}
