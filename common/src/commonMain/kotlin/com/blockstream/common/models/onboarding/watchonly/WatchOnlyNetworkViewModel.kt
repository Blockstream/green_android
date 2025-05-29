package com.blockstream.common.models.onboarding.watchonly

import com.blockstream.common.data.SetupArgs
import com.blockstream.common.extensions.previewNetwork
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.ui.events.Event
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

abstract class WatchOnlyNetworkViewModelAbstract(
    val setupArgs: SetupArgs
) : GreenViewModel(greenWalletOrNull = setupArgs.greenWallet) {
    override fun screenName(): String = "OnBoardChooseNetwork"

    @NativeCoroutinesState
    abstract val networks: StateFlow<List<Network>>
}

class WatchOnlyNetworkViewModel(setupArgs: SetupArgs) :
    WatchOnlyNetworkViewModelAbstract(setupArgs = setupArgs) {

    class LocalEvents {
        data class ChooseNetwork(val network: Network) : Event
    }

    private val _networks: MutableStateFlow<List<Network>> = MutableStateFlow(listOf())
    override val networks: StateFlow<List<Network>> = _networks.asStateFlow()

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.ChooseNetwork) {
            postSideEffect(SideEffects.NavigateTo(NavigateDestinations.WatchOnlyCredentials(setupArgs.copy(network = event.network))))
        }
    }

    init {

        settingsManager.appSettingsStateFlow.onEach {
            val testnet = it.testnet

            if (setupArgs.isSinglesig == true) {
                _networks.value = listOfNotNull(
                    session.networks.bitcoinElectrum,
                    session.networks.liquidElectrum,
                    if (testnet) session.networks.testnetBitcoinElectrum else null,
                    if (testnet) session.networks.testnetLiquidElectrum else null
                )
            } else {
                _networks.value = listOfNotNull(
                    session.networks.bitcoinGreen,
                    session.networks.liquidGreen
                ) + if (testnet) listOf(
                    session.networks.testnetBitcoinGreen,
                    session.networks.testnetLiquidGreen
                ) else listOf()
            }

        }.launchIn(viewModelScope.coroutineScope)

        bootstrap()
    }
}

class WatchOnlyNetworkViewModelPreview(setupArgs: SetupArgs) :
    WatchOnlyNetworkViewModelAbstract(setupArgs = setupArgs) {
    override val networks: StateFlow<List<Network>> = MutableStateFlow(listOf(previewNetwork(), previewNetwork(false)))

    companion object {
        fun preview() = WatchOnlyNetworkViewModelPreview(
            setupArgs = SetupArgs(greenWallet = previewWallet(isHardware = true))
        )
    }
}


