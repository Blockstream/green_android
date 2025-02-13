package com.blockstream.common.models.onboarding.phone

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_creating_wallet
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestination
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.usecases.NewWalletUseCase
import com.blockstream.green.utils.Loggable
import com.blockstream.ui.events.Event
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

abstract class AddWalletViewModelAbstract() : GreenViewModel()

class AddWalletViewModel :
    AddWalletViewModelAbstract() {
    override fun screenName(): String = "AddWallet"

    private val newWalletUseCase: NewWalletUseCase by inject()

    class LocalEvents {
        data object NewWallet : Event
        data object RestoreWallet : Event
        data class SelectEnviroment(val isTestnet: Boolean, val customNetwork: Network?) : Event
    }

    private val isTestnetEnabled
        get() = settingsManager.getApplicationSettings().testnet

    private var pendingDestination: NavigateDestination? = null

    init {
        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.NewWallet -> {
                if (isTestnetEnabled) {
                    postSideEffect(SideEffects.NavigateTo(NavigateDestinations.Environment))
                } else {
                    createNewWallet(isTestnet = false)
                }
                countly.newWallet()
            }

            is LocalEvents.RestoreWallet -> {
                SideEffects.NavigateTo(
                    NavigateDestinations.EnterRecoveryPhrase(
                        setupArgs = SetupArgs(
                            isRestoreFlow = true
                        )
                    )
                ).also {
                    if (isTestnetEnabled) {
                        pendingDestination = it.destination
                        postSideEffect(SideEffects.NavigateTo(NavigateDestinations.Environment))
                    } else {
                        postSideEffect(it)
                    }
                }
                countly.restoreWallet()
            }

            is LocalEvents.SelectEnviroment -> {
                pendingDestination?.let {
                    when (it) {
                        is NavigateDestinations.SetPin -> {
                            it.copy(
                                setupArgs = it.setupArgs.copy(
                                    isTestnet = event.isTestnet, network = event.customNetwork
                                )
                            )
                        }

                        is NavigateDestinations.RecoveryIntro -> {
                            it.copy(
                                setupArgs = it.setupArgs.copy(
                                    isTestnet = event.isTestnet, network = event.customNetwork
                                )
                            )
                        }

                        is NavigateDestinations.EnterRecoveryPhrase -> {
                            it.copy(
                                setupArgs = it.setupArgs.copy(
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
                } ?: run {
                    createNewWallet(isTestnet = event.isTestnet)
                }
            }
        }
    }

    private fun createNewWallet(isTestnet: Boolean = false) {
        doAsync({
            onProgressDescription.value = getString(Res.string.id_creating_wallet)
//            newWalletUseCase(session = session, "0000", isTestnet = isTestnet)
        }, preAction = {
            onProgress.value = true
            // rocketAnimation.value = true
        }, postAction = {
            onProgress.value = it == null
            // rocketAnimation.value = it == null
        }, onSuccess = {
            //postSideEffect(SideEffects.NavigateTo(NavigateDestinations.WalletOverview(it)))
        })
    }

    companion object : Loggable()
}

class AddWalletViewModelPreview() : AddWalletViewModelAbstract() {

    companion object {
        fun preview() = AddWalletViewModelPreview()
    }
}