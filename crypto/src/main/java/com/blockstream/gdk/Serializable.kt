package com.blockstream.gdk


import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

abstract class GAJson<T> {
    open val encodeDefaultsValues = true

    abstract fun kSerializer(): KSerializer<T>

    final override fun toString(): String {
        @Suppress("UNCHECKED_CAST")
        return Json{encodeDefaults = encodeDefaultsValues}.encodeToString(kSerializer(), this as T)
    }
}