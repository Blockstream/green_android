package com.blockstream.common.models.onboarding

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_creating_wallet
import blockstream_green.common.generated.resources.id_set_up_watchonly
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Eye
import com.blockstream.common.Urls
import com.blockstream.common.crypto.BiometricsException
import com.blockstream.common.crypto.PlatformCipher
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Events
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.usecases.NewWalletUseCase
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.NavAction
import com.blockstream.ui.navigation.NavData
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

abstract class SetupNewWalletViewModelAbstract(greenWalletOrNull: GreenWallet? = null) :
    GreenViewModel(greenWalletOrNull = greenWalletOrNull) {
    override fun screenName(): String = "SetupNewWallet"

    fun onSetupNewWallet() {
        postEvent(SetupNewWalletViewModel.LocalEvents.SetupMobileWallet)
        countly.setupSww()
    }

}

class SetupNewWalletViewModel(greenWalletOrNull: GreenWallet? = null) :
    SetupNewWalletViewModelAbstract(greenWalletOrNull = greenWalletOrNull) {
    private val newWalletUseCase: NewWalletUseCase by inject()

    private var _activeEvent: Event? = null

    class LocalEvents {
        object ClickOnThisDevice : Event
        object ClickOnHardwareWallet : Event
        object WatchOnly : Event

        object SetupMobileWallet : Event
        object RestoreWallet : Event

        object SetupHardwareWallet : Event
        object BuyJade : Events.OpenBrowser(Urls.JADE_STORE)
    }

    init {
        viewModelScope.launch {
            _navData.value = NavData(
                actions = listOfNotNull(
                    NavAction(
                        titleRes = Res.string.id_set_up_watchonly,
                        imageVector = PhosphorIcons.Regular.Eye,
                        isMenuEntry = true,
                    ) {
                        postSideEffect(SideEffects.NavigateTo(NavigateDestinations.WatchOnlyPolicy))
                    }
                )
            )
        }
        bootstrap()
    }

    private val isTestnetEnabled
        get() = settingsManager.getApplicationSettings().testnet

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.SetupMobileWallet -> {
                countly.newWallet()
                handleActions(event)
            }

            is LocalEvents.RestoreWallet -> {
                countly.restoreWallet()
                handleActions(event)
            }

            is LocalEvents.SetupHardwareWallet -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.UseHardwareDevice))
            }

            is LocalEvents.ClickOnThisDevice -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.AddWallet))
                countly.addWallet()
            }

            is LocalEvents.ClickOnHardwareWallet -> {
                countly.hardwareWallet()
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.UseHardwareDevice))
            }

            is LocalEvents.WatchOnly -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.WatchOnlyPolicy))
                countly.watchOnlyWallet()
            }

            is Events.SelectEnviroment -> {
                if (_activeEvent == LocalEvents.SetupMobileWallet) {
                    createWallet(isTestnet = event.isTestnet)
                } else if (_activeEvent == LocalEvents.RestoreWallet) {
                    restoreWallet(isTestnet = event.isTestnet)
                }
            }
        }
    }

    private fun handleActions(event: Event) {
        _activeEvent = event

        when (event) {
            is LocalEvents.SetupMobileWallet -> {
                if (isTestnetEnabled) {
                    postSideEffect(SideEffects.NavigateTo(NavigateDestinations.Environment))
                } else {
                    createWallet()
                }
            }

            is LocalEvents.RestoreWallet -> {
                if (isTestnetEnabled) {
                    postSideEffect(SideEffects.NavigateTo(NavigateDestinations.Environment))
                } else {
                    restoreWallet()
                }
            }
        }

    }

    private fun createWallet(isTestnet: Boolean = false) {

        if (!greenKeystore.canUseBiometrics()) {
            postSideEffect(SideEffects.NavigateTo(NavigateDestinations.SetPin(SetupArgs(isTestnet = isTestnet))))
            return
        }

        val biometricsCipherProvider = viewModelScope.coroutineScope.async(
            start = CoroutineStart.LAZY
        ) {
            CompletableDeferred<PlatformCipher>().let {
                biometricsPlatformCipher = it
                postSideEffect(SideEffects.RequestBiometricsCipher)
                it.await()
            }
        }

        doAsync({
            onProgressDescription.value = getString(Res.string.id_creating_wallet)

            try {
                val cipher = if (greenKeystore.canUseBiometrics()) {
                    biometricsCipherProvider.await()
                } else null

                newWalletUseCase.invoke(
                    session = session,
                    cipher = cipher,
                    isTestnet = isTestnet
                )
            } catch (e: Exception) {
                if (e.message == "id_action_canceled" || e is BiometricsException) {
                    null
                } else {
                    throw e
                }
            }
        }, onSuccess = { greenWallet ->
            if (greenWallet != null) {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.WalletOverview(
                            greenWallet = greenWallet,
                            showWalletOnboarding = true
                        )
                    )
                )
            } else {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.SetPin(
                            SetupArgs(
                                isTestnet = isTestnet
                            )
                        )
                    )
                )
            }
        })
    }

    private fun restoreWallet(isTestnet: Boolean = false) {
        postSideEffect(
            SideEffects.NavigateTo(
                NavigateDestinations.EnterRecoveryPhrase(
                    setupArgs = SetupArgs(
                        isRestoreFlow = true,
                        isTestnet = isTestnet
                    )
                )
            )
        )
    }
}

class SetupNewWalletViewModelPreview() : SetupNewWalletViewModelAbstract() {

    companion object {
        fun preview() = SetupNewWalletViewModelPreview()
    }
}