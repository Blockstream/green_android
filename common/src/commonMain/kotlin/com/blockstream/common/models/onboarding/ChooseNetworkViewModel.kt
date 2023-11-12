package com.blockstream.common.models.onboarding

import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Event
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.rickclephas.kmm.viewmodel.coroutineScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

abstract class ChooseNetworkViewModelAbstract(
    val setupArgs: SetupArgs
) : GreenViewModel(greenWalletOrNull = setupArgs.greenWallet) {
    override fun screenName(): String = "OnBoardChooseNetwork"

    @NativeCoroutinesState
    abstract val mainNetworks: StateFlow<List<Network>>
    @NativeCoroutinesState
    abstract val additionalNetworks: StateFlow<List<Network>>
}

class ChooseNetworkViewModel(setupArgs: SetupArgs) :
    ChooseNetworkViewModelAbstract(setupArgs = setupArgs) {

    class LocalEvents {
        data class ChooseNetwork(val network: Network) : Event
    }

    private val _mainNetworks: MutableStateFlow<List<Network>> = MutableStateFlow(listOf())
    override val mainNetworks: StateFlow<List<Network>> = _mainNetworks.asStateFlow()

    private val _additionalNetworks: MutableStateFlow<List<Network>> = MutableStateFlow(listOf())
    override val additionalNetworks: StateFlow<List<Network>> = _additionalNetworks.asStateFlow()

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if(event is LocalEvents.ChooseNetwork){
            postSideEffect(SideEffects.Navigate(setupArgs.copy(network = event.network)))
        }
    }

    init {

        settingsManager.appSettingsStateFlow.onEach {
            val testnet = it.testnet
            if (setupArgs.isSinglesig == true) {
                _mainNetworks.value = listOf(session.networks.bitcoinElectrum)
                _additionalNetworks.value = listOfNotNull(if (testnet) session.networks.testnetBitcoinElectrum else null)
            } else {
                _mainNetworks.value = listOf(session.networks.bitcoinGreen, session.networks.liquidGreen)
                _additionalNetworks.value = if(testnet) listOf(session.networks.testnetBitcoinGreen, session.networks.testnetLiquidGreen) else listOf()
            }

        }.launchIn(viewModelScope.coroutineScope)

        session.networks.liquidGreen

        bootstrap()
    }
}

class ChooseNetworkViewModelPreview(setupArgs: SetupArgs) :
    ChooseNetworkViewModelAbstract(setupArgs = setupArgs) {
    companion object {
        fun preview() = ChooseNetworkViewModelPreview(
            setupArgs = SetupArgs(greenWallet = previewWallet(isHardware = true))
        )
    }

    override val mainNetworks: StateFlow<List<Network>> = MutableStateFlow(listOf())
    override val additionalNetworks: StateFlow<List<Network>> = MutableStateFlow(listOf())
}


