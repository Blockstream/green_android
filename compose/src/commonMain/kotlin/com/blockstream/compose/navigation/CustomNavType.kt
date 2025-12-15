@file:OptIn(ExperimentalEncodingApi::class, InternalSerializationApi::class)

package com.blockstream.compose.navigation

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.reflect.KClass

class CustomNavType<T : Any> @PublishedApi internal constructor(
    private val kClass: KClass<T>,
    isNullableAllowed: Boolean = true
) : NavType<T?>(isNullableAllowed = isNullableAllowed) {

    companion object {
        inline fun <reified T : Any> create(isNullableAllowed: Boolean = true) = CustomNavType(T::class, isNullableAllowed)
    }

    override fun parseValue(value: String): T {
        return Base64.UrlSafe.decode(value.encodeToByteArray()).decodeToString().let {
            GreenJson.json.decodeFromString(kClass.serializer(), it)
        }
    }

    override fun serializeAsValue(value: T?): String {
        return value?.let {
            GreenJson.json.encodeToString(kClass.serializer(), value).let {
                Base64.UrlSafe.encode(it.encodeToByteArray())
            }
        } ?: "null"
    }

    override fun get(bundle: SavedState, key: String): T? {
        return parseValue(bundle.read { getStringOrNull(key) } ?: return null)
    }

    override fun put(bundle: SavedState, key: String, value: T?) {
        bundle.write {
            putString(key, serializeAsValue(value))
        }
    }
}