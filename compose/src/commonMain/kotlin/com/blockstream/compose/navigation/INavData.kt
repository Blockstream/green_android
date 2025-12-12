package com.blockstream.compose.navigation

import kotlinx.coroutines.flow.StateFlow

interface INavData {
    val navData: StateFlow<NavData>

    fun navigateBack()
}