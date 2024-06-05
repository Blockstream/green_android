@file:OptIn(InternalVoyagerApi::class)

package com.blockstream.compose.sheets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.stack.Stack
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.compositionUniqueId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


typealias BottomSheetNavigatorContent = @Composable (bottomSheetNavigator: BottomSheetNavigatorM3) -> Unit

val LocalBottomSheetNavigatorM3: ProvidableCompositionLocal<BottomSheetNavigatorM3?> =
    staticCompositionLocalOf { null }

@Composable
fun BottomSheetNavigatorM3(
    key: String = compositionUniqueId(),
    bottomSheetContent: BottomSheetNavigatorContent = { CurrentScreen() },
    content: BottomSheetNavigatorContent
) {
    val coroutineScope = rememberCoroutineScope()

    Navigator(EmptyBottomSheetScreen, onBackPressed = null, key = key) { navigator ->
        val bottomSheetNavigator = remember(navigator, coroutineScope) {
            BottomSheetNavigatorM3(navigator, coroutineScope)
        }

        CompositionLocalProvider(LocalBottomSheetNavigatorM3 provides bottomSheetNavigator) {
            content(bottomSheetNavigator)
            bottomSheetContent(bottomSheetNavigator)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
class BottomSheetNavigatorM3 constructor(
    private val navigator: Navigator,
    private val coroutineScope: CoroutineScope
) : Stack<Screen> by navigator {

    fun show(screen: Screen) {
        coroutineScope.launch {
            push(screen)
        }
    }
    
    public fun hide() {
        coroutineScope.launch {
            (lastItemOrNull as? BottomScreen)?.sheetState?.also {
                it.hide()
            }

            if(size > 1) {
                pop()
            }
        }
    }

    @Composable
    public fun saveableState(
        key: String,
        content: @Composable () -> Unit
    ) {
        navigator.saveableState(key, content = content)
    }
}

private object EmptyBottomSheetScreen : Screen {

    @Composable
    override fun Content() {}
}