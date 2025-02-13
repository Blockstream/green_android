package com.blockstream.common.managers

import java.util.Locale

actual class LocaleManager {

    actual fun getLocale(): String? {
        return null
    }

    actual fun setLocale(locale: String?) {
    }

    actual fun getCountry() = Locale.getDefault().country
}