package com.blockstream.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import cafe.adriel.voyager.core.screen.Screen
import com.blockstream.common.data.NavData
import com.blockstream.compose.LocalAppBarState
import com.blockstream.compose.LocalRootNavigator

@Stable
class AppBarState(navData: NavData = NavData()) {
    var data = mutableStateOf(navData)
        private set

    fun update(data : NavData){
        this.data.value = data
    }
}

@Composable
fun Screen.AppBar(navData: NavData) {
    val screen = this
    val localAppBarState = LocalAppBarState.current
    val navigator = LocalRootNavigator.current

    val key = navData.hashCode() + (navigator?.lastItemOrNull?.hashCode() ?: 0)

    LaunchedEffect(key) {
        if(navigator?.lastItemOrNull == screen){
            localAppBarState.update(navData)
        }
    }
}