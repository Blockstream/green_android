package com.blockstream.common.models.add

import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.Wally
import com.blockstream.common.sideeffects.SideEffects
import com.rickclephas.kmm.viewmodel.coroutineScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

abstract class XpubViewModelAbstract(val setupArgs: SetupArgs) : AddAccountViewModelAbstract(greenWallet = setupArgs.greenWallet!!) {
    override fun screenName(): String = "AddAccountPublicKey"

    @NativeCoroutinesState
    abstract val xpub: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val isXpubValid: MutableStateFlow<Boolean>
}

class XpubViewModel(setupArgs: SetupArgs) : XpubViewModelAbstract(setupArgs = setupArgs) {

    override val xpub: MutableStateFlow<String> = MutableStateFlow("")
    override val isXpubValid: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val wally : Wally by inject()

    init {
        xpub.onEach {
            isXpubValid.value = withContext(Dispatchers.IO){
                wally.isXpubValid(it)
            }
        }.launchIn(viewModelScope.coroutineScope)

        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)
        if(event is Events.Continue){
            postSideEffect(SideEffects.Navigate(setupArgs.copy(xpub = xpub.value)))
        }
    }
}

class XpubViewModelPreview(setupArgs: SetupArgs) : XpubViewModelAbstract(setupArgs = setupArgs) {

    override val xpub: MutableStateFlow<String> = MutableStateFlow("")

    override val isXpubValid: MutableStateFlow<Boolean> = MutableStateFlow(false)

    companion object {
        fun preview() = XpubViewModelPreview(
            setupArgs = SetupArgs(greenWallet = previewWallet(isHardware = true))
        )
    }
}


