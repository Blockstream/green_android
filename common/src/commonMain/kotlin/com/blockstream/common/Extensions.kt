package com.blockstream.common

import com.russhwolf.settings.Settings

fun Settings.putString(key: String, value: String?) {
    if (value == null) {
        this.remove(key)
    } else {
        putString(key, value)
    }
}