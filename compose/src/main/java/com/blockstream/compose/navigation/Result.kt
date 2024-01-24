package com.blockstream.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import co.touchlab.kermit.Logger
import kotlin.reflect.KClass


/**
 * Navigator
 */
private val results = mutableStateMapOf<String, Any?>()

val Screen.resultKey get() = this::class.resultKey

val KClass<*>.resultKey: String get() = "${qualifiedName ?: error("QualifiedName is not accessible")}_RESULT"

fun setNavigationResult(key: String, result: Any?) {
    Logger.d { "setResult for key $key" }
    results[key] = result
}

fun Navigator.pushUnique(screen: Screen) {
    val existingScreen = items.firstOrNull { it.key == screen.key }
    if (existingScreen == null) {
        push(screen)
    }else{
        Logger.d { "Navigator: there is already a screen with key ${screen.key} in the stack" }
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
fun <T> getNavigationResult(screenKey: String): State<T?> {
    val result = results[screenKey] as? T
    val resultState = remember(screenKey, result) {
        derivedStateOf {
            results.remove(screenKey)
            result
        }
    }
    return resultState
}
