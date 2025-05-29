package com.blockstream.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import kotlin.reflect.KClass

val results = mutableStateMapOf<String, Any?>()

val KClass<*>.resultKey: String get() = "${qualifiedName?.removeSuffix(".Companion") ?: error("QualifiedName is not accessible")}_RESULT"

fun setNavigationResultForKey(key: String, result: Any?) {
    Logger.d { "setResult for key $key" }
    results[key] = result
}

@Composable
inline fun <reified T> getNavigationResult(screenKey: String): State<T?> {
    val result = results[screenKey] as? T
    val resultState = remember(screenKey, result) {
        derivedStateOf {
            results.remove(screenKey)
            result
        }
    }
    return resultState
}

@Composable
inline fun <reified T> getNavigationResult(kClass: KClass<*>, fn: (T) -> Unit) =
    getNavigationResultForKey<T>(kClass.resultKey, fn)

@Composable
inline fun <reified T> getNavigationResultForKey(screenKey: String, fn: (T) -> Unit) =
    getNavigationResult<T>(screenKey).value?.also {
        fn(it)
    }

inline fun <reified T> setNavigationResult(kClass: KClass<*>, result: T) = setNavigationResultForKey(kClass.resultKey, result)

inline fun <reified T> Any.setResult(result: T) {
    setNavigationResult(this::class, result)
}

@Composable
inline fun <reified T> Any.getResult(fn: (result: T) -> Unit) {
    getNavigationResult(this::class, fn)
}