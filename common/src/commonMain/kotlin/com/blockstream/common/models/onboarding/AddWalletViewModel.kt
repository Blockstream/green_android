package com.blockstream.common.models.onboarding

import com.blockstream.common.data.SetupArgs
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.events.Event
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestination
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects

abstract class AddWalletViewModelAbstract() : GreenViewModel()

class AddWalletViewModel : AddWalletViewModelAbstract() {
    override fun screenName(): String = "AddWallet"

    class LocalEvents {
        class ClickNewWallet : Event
        class ClickRestoreWallet : Event
        class ClickWatchOnly : Event
        class SelectEnviroment(val pending: NavigateDestination, val isTestnet: Boolean, val customNetwork: Network?):
            Event
    }

    sealed class LocalSideEffects{
        class SelectEnvironment(val pending: NavigateDestination): SideEffect
    }

    val isTestnetEnabled
        get() = settingsManager.getApplicationSettings().testnet

    init {
        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ClickNewWallet -> {
                SideEffects.NavigateTo(NavigateDestinations.NewWallet(args = SetupArgs(isRestoreFlow = false))).also {
                    if(isTestnetEnabled){
                        postSideEffect(LocalSideEffects.SelectEnvironment(it.destination))
                    }else{
                        postSideEffect(it)
                    }
                }
                countly.newWallet()
            }

            is LocalEvents.ClickRestoreWallet -> {
                SideEffects.NavigateTo(NavigateDestinations.RestoreWallet(args = SetupArgs(isRestoreFlow = true))).also {
                    if(isTestnetEnabled){
                        postSideEffect(LocalSideEffects.SelectEnvironment(it.destination))
                    }else{
                        postSideEffect(it)
                    }
                }
                countly.restoreWallet()
            }

            is LocalEvents.ClickWatchOnly -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.NewWatchOnlyWallet))
                countly.watchOnlyWallet()
            }

            is LocalEvents.SelectEnviroment -> {
                event.pending.let {
                    when (it) {
                        is NavigateDestinations.NewWallet -> {
                            it.copy(
                                args = it.args.copy(
                                    isTestnet = event.isTestnet, network = event.customNetwork
                                )
                            )
                        }

                        is NavigateDestinations.RestoreWallet -> {
                            it.copy(
                                args = it.args.copy(
                                    isTestnet = event.isTestnet, network = event.customNetwork
                                )
                            )
                        }
                        else -> {
                            null
                        }
                    }
                }?.also {
                    postSideEffect(SideEffects.NavigateTo(it))
                }
            }
        }
    }
}

class AddWalletWalletViewModelPreview() : AddWalletViewModelAbstract() {

    companion object {
        fun preview() = AddWalletWalletViewModelPreview()
    }
}