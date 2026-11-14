package com.blockstream.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import co.touchlab.kermit.Logger
import kotlin.reflect.KClass

val results = mutableStateMapOf<String, Any?>()

val KClass<*>.resultKey: String get() = "${qualifiedName?.removeSuffix(".Companion") ?: error("QualifiedName is not accessible")}_RESULT"

fun setNavigationResultForKey(key: String, result: Any?) {
    Logger.d { "setResult for key $key" }
    results[key] = result
}

@Composable
inline fun <reified T> getNavigationResultForKey(screenKey: String, crossinline fn: (T) -> Unit) {
    val result = results[screenKey] as? T
    LaunchedEffect(screenKey, result) {
        if (result != null) {
            results.remove(screenKey)
            fn(result)
        }
    }
}

@Composable
inline fun <reified T> getNavigationResult(kClass: KClass<*>, crossinline fn: (T) -> Unit) =
    getNavigationResultForKey<T>(kClass.resultKey, fn)

inline fun <reified T> setNavigationResult(kClass: KClass<*>, result: T) = setNavigationResultForKey(kClass.resultKey, result)

inline fun <reified T> Any.setResult(result: T) {
    setNavigationResult(this::class, result)
}

@Composable
inline fun <reified T> Any.getResult(crossinline fn: (result: T) -> Unit) {
    getNavigationResult(this::class, fn)
}