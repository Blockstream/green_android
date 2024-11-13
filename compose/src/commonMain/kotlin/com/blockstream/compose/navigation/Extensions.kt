package com.blockstream.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.stack.Stack
import cafe.adriel.voyager.navigator.Navigator
import co.touchlab.kermit.Logger
import kotlin.reflect.KClass


/**
 * Navigator
 */
val results = mutableStateMapOf<String, Any?>()

val KClass<*>.resultKey: String get() = "${qualifiedName ?: error("QualifiedName is not accessible")}_RESULT"

fun setNavigationResultForKey(key: String, result: Any?) {
    Logger.d { "setResult for key $key" }
    results[key] = result
}

fun <Item : Screen> Stack<Item>.pushOrReplace(item: Item) {
    if(lastItemOrNull?.let { it::class == item::class } == true) {
        replace(item)
    } else {
        push(item)
    }
}

fun Navigator.pushUnique(screen: Screen) {
    val existingScreen = items.firstOrNull { it.key == screen.key }
    if (existingScreen == null) {
        push(screen)
    }else{
        Logger.d { "Navigator: there is already a screen with key ${screen.key} in the stack" }
    }
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

