package com.blockstream.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.blockstream.compose.LocalAppBarState
import com.blockstream.compose.components.MenuEntry

@Immutable
data class AppBarData(
    val title: String? = null,
    val subtitle: String? = null,
    val hide: Boolean = false,
    val menu: List<MenuEntry>? = null,
    val onBack: (() -> Unit)? = null
)

@Stable
class AppBarState(appBarData: AppBarData = AppBarData()) {
    var data = mutableStateOf(appBarData)
        private set

    fun update(data : AppBarData){
        this.data.value = data
    }
}

@Composable
fun Screen.AppBar(fn: @Composable () -> AppBarData = { AppBarData() }) {
    val screen = this
    val localAppBarState = LocalAppBarState.current
    val navigator = LocalNavigator.current

    val appBarData = fn.invoke()

    val key = appBarData.hashCode() + (navigator?.lastItemOrNull?.hashCode() ?: 0)

    LaunchedEffect(key) {
        if(navigator?.lastItemOrNull == screen){
            localAppBarState.update(appBarData)
        }
    }
}