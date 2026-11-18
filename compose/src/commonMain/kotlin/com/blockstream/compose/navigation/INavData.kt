package com.blockstream.compose.navigation

import com.blockstream.compose.models.IPostEvent
import kotlinx.coroutines.flow.StateFlow

interface INavData : IPostEvent {
    val navData: StateFlow<NavData>

    fun navigateBack()
}