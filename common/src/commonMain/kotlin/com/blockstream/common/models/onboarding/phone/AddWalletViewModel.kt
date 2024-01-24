package com.blockstream.common.models.onboarding.phone

import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Event
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestination
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable

abstract class AddWalletViewModelAbstract() : GreenViewModel()

class AddWalletViewModel : AddWalletViewModelAbstract() {
    override fun screenName(): String = "AddWallet"

    class LocalEvents {
        object NewWallet : Event
        object RestoreWallet : Event
        class SelectEnviroment(val isTestnet: Boolean, val customNetwork: Network?): Event
    }

    sealed class LocalSideEffects{
        object SelectEnvironment: SideEffect
    }

    private val isTestnetEnabled
        get() = settingsManager.getApplicationSettings().testnet

    private var pendingDestination: NavigateDestination? = null

    init {
        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.NewWallet -> {
                SideEffects.NavigateTo(NavigateDestinations.RecoveryIntro(args = SetupArgs(isRestoreFlow = false))).also {
                    if(isTestnetEnabled){
                        pendingDestination = it.destination
                        postSideEffect(LocalSideEffects.SelectEnvironment)
                    }else{
                        postSideEffect(it)
                    }
                }
                countly.newWallet()
            }

            is LocalEvents.RestoreWallet -> {
                SideEffects.NavigateTo(NavigateDestinations.EnterRecoveryPhrase(args = SetupArgs(isRestoreFlow = true))).also {
                    if(isTestnetEnabled){
                        pendingDestination = it.destination
                        postSideEffect(LocalSideEffects.SelectEnvironment)
                    }else{
                        postSideEffect(it)
                    }
                }
                countly.restoreWallet()
            }

            is LocalEvents.SelectEnviroment -> {
                pendingDestination.let {
                    when (it) {
                        is NavigateDestinations.RecoveryIntro -> {
                            it.copy(
                                args = it.args.copy(
                                    isTestnet = event.isTestnet, network = event.customNetwork
                                )
                            )
                        }

                        is NavigateDestinations.EnterRecoveryPhrase -> {
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

    companion object: Loggable()
}

class AddWalletViewModelPreview() : AddWalletViewModelAbstract() {

    companion object {
        fun preview() = AddWalletViewModelPreview()
    }
}