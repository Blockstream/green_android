@file:OptIn(ExperimentalObjCName::class)

package com.blockstream.ui.models

import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.INavData
import com.blockstream.ui.navigation.NavData
import com.blockstream.ui.sideeffects.SideEffect
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesIgnore
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

interface IOnProgress {
    @NativeCoroutinesState
    val onProgress: StateFlow<Boolean>

    @NativeCoroutinesState
    val onProgressDescription: StateFlow<String?>
}

interface IPostEvent {
    @ObjCName(name = "post", swiftName = "postEvent")
    fun postEvent(
        @ObjCName(swiftName = "_")
        event: Event
    )
}

abstract class BaseViewModel() : ViewModel(), INavData, IOnProgress, IPostEvent {
    protected val _event: MutableSharedFlow<Event> = MutableSharedFlow()

    protected val _sideEffect: Channel<SideEffect> = Channel()

    @NativeCoroutines
    val sideEffect: Flow<SideEffect> = _sideEffect.receiveAsFlow()

    protected val _navData = MutableStateFlow(NavData())

    @NativeCoroutinesState
    override val navData: StateFlow<NavData> = _navData

    @NativeCoroutinesState
    override val onProgress: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, false)

    @NativeCoroutinesState
    override val onProgressDescription: MutableStateFlow<String?> = MutableStateFlow(null)

    // Helper method to force the use of MutableStateFlow(viewModelScope)
    @NativeCoroutinesIgnore
    protected fun <T> MutableStateFlow(value: T) =
        MutableStateFlow(viewModelScope, value)

    override fun postEvent(event: Event) {
        viewModelScope.coroutineScope.launch { _event.emit(event) }
    }

    protected open fun postSideEffect(sideEffect: SideEffect) {
        viewModelScope.coroutineScope.launch {
            _sideEffect.send(sideEffect)
        }
    }

}