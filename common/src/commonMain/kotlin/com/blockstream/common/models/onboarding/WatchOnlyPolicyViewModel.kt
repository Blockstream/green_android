package com.blockstream.common.models.onboarding

import com.blockstream.common.data.SetupArgs
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.events.Event
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestination
import com.blockstream.common.sideeffects.SideEffects
import org.koin.core.component.inject

abstract class WatchOnlyPolicyViewModelAbstract() : GreenViewModel()

class WatchOnlyPolicyViewModel : WatchOnlyPolicyViewModelAbstract() {
    override fun screenName(): String = "OnBoardWatchOnlyChooseSecurity"

    val gdk: Gdk by inject()

    class LocalEvents {
        class SelectPolicy(val isMultisig: Boolean) : Event
    }

    class Destination : NavigateDestination {
        data class ChooseNetwork(val setupArgs: SetupArgs) : NavigateDestination

        data class Singlesig(val setupArgs: SetupArgs) : NavigateDestination
    }

    init {
        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)
        if (event is LocalEvents.SelectPolicy) {
            if (event.isMultisig) {
                postSideEffect(
                    SideEffects.NavigateTo(
                        Destination.ChooseNetwork(
                            SetupArgs(
                                isRestoreFlow = true,
                                isWatchOnly = true,
                                isSinglesig = false
                            )
                        )
                    )
                )
            } else {
                if (settingsManager.getApplicationSettings().testnet) {
                    postSideEffect(
                        SideEffects.NavigateTo(
                            Destination.ChooseNetwork(
                                SetupArgs(
                                    isRestoreFlow = true,
                                    isWatchOnly = true,
                                    isSinglesig = true
                                )
                            )
                        )
                    )
                } else {
                    postSideEffect(
                        SideEffects.NavigateTo(
                            Destination.Singlesig(
                                SetupArgs(
                                    isRestoreFlow = true,
                                    isWatchOnly = true,
                                    isSinglesig = true,
                                    isTestnet = false,
                                    network = gdk.networks().bitcoinElectrum
                                )
                            )
                        )
                    )
                }
            }
        }
    }
}