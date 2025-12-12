@file:OptIn(ExperimentalObjCName::class)

package com.blockstream.compose.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blockstream.compose.events.Event
import com.blockstream.compose.navigation.INavData
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.sideeffects.SideEffect
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.experimental.ExperimentalObjCName

interface IOnProgress {
    val onProgress: StateFlow<Boolean>

    val onProgressDescription: StateFlow<String?>
}

interface IPostEvent {
    fun postEvent(
        event: Event
    )
}

abstract class BaseViewModel() : ViewModel(), INavData, IOnProgress, IPostEvent {
    protected val _event: MutableSharedFlow<Event> = MutableSharedFlow()

    protected val _sideEffect: Channel<SideEffect> = Channel()

    val sideEffect: Flow<SideEffect> = _sideEffect.receiveAsFlow()

    protected val _navData = MutableStateFlow(NavData())

    override val navData: StateFlow<NavData> = _navData

    override val onProgress: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val onProgressDescription: MutableStateFlow<String?> = MutableStateFlow(null)

    override fun postEvent(event: Event) {
        viewModelScope.launch { _event.emit(event) }
    }

    protected open fun postSideEffect(sideEffect: SideEffect) {
        viewModelScope.launch {
            _sideEffect.send(sideEffect)
        }
    }

}