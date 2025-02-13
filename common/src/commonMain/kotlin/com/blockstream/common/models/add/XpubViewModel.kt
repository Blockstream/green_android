package com.blockstream.common.models.add

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_invalid_xpub
import com.blockstream.ui.navigation.NavData
import com.blockstream.common.data.SetupArgs
import com.blockstream.ui.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isBlank
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.Wally
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

abstract class XpubViewModelAbstract(val setupArgs: SetupArgs) : AddAccountViewModelAbstract(
    greenWallet = setupArgs.greenWallet!!,
    assetId = setupArgs.assetId,
    popTo = setupArgs.popTo
) {
    override fun screenName(): String = "AddAccountPublicKey"

    @NativeCoroutinesState
    abstract val xpub: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val error: StateFlow<String?>
}

class XpubViewModel(setupArgs: SetupArgs) : XpubViewModelAbstract(setupArgs = setupArgs) {

    override val xpub: MutableStateFlow<String> = MutableStateFlow("")
    private val _error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private val wally: Wally by inject()

    init {
        viewModelScope.launch {
            _navData.value = NavData(title = setupArgs.accountType?.toString(), subtitle = greenWallet.name)
        }

        xpub.onEach {
            if (it.isBlank()) {
                _error.value = null
                _isValid.value = false
            } else {
                _isValid.value = withContext(Dispatchers.IO) {
                    wally.isXpubValid(it)
                }.also { isXpubValid ->
                    _error.value = if (!isXpubValid) getString(Res.string.id_invalid_xpub) else null
                }
            }

        }.launchIn(viewModelScope.coroutineScope)

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        if (event is Events.Continue) {
            postSideEffect(SideEffects.NavigateTo(NavigateDestinations.ReviewAddAccount(setupArgs.copy(xpub = xpub.value))))
        }
    }
}

class XpubViewModelPreview(setupArgs: SetupArgs) : XpubViewModelAbstract(setupArgs = setupArgs) {

    override val xpub: MutableStateFlow<String> = MutableStateFlow("")

    override val error: MutableStateFlow<String?> = MutableStateFlow(null)

    companion object {
        fun preview() = XpubViewModelPreview(
            setupArgs = SetupArgs(greenWallet = previewWallet(isHardware = true))
        )
    }
}


