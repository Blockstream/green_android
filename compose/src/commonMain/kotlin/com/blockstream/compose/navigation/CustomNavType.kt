@file:OptIn(ExperimentalEncodingApi::class, InternalSerializationApi::class)

package com.blockstream.compose.navigation

import androidx.core.bundle.Bundle
import androidx.navigation.NavType
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.reflect.KClass

class CustomNavType<T: Any> @PublishedApi internal constructor(
    private val kClass: KClass<T>,
    isNullableAllowed : Boolean = true
) : NavType<T?>(isNullableAllowed = isNullableAllowed) {

    companion object {
        inline fun <reified T: Any> create(isNullableAllowed : Boolean = true) = CustomNavType(T::class, isNullableAllowed)
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

    override fun get(bundle: Bundle, key: String): T? {
        return parseValue(bundle.getString(key) ?: return null)
    }

    override fun put(bundle: Bundle, key: String, value: T?) {
        bundle.putString(key, serializeAsValue(value))
    }
}