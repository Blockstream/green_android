package com.blockstream.common.managers

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
}
