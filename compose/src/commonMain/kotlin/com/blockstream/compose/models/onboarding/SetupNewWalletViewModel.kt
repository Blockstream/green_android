package com.blockstream.compose.models.onboarding

import androidx.lifecycle.viewModelScope
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
import com.blockstream.common.extensions.launchIn
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavAction
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.domain.wallet.NewWalletUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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
                        isMenuEntry = false,
                    ) {
                        postSideEffect(SideEffects.NavigateTo(NavigateDestinations.WatchOnlySinglesig(
                            SetupArgs(
                                isRestoreFlow = true,
                                isWatchOnly = true,
                                isSinglesig = true,
                            )
                        )))
                    }
                )
            )
        }

        onProgress.onEach {
            _navData.value = _navData.value.copy(isVisible = !it)
        }.launchIn(this)

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

            is LocalEvents.WatchOnly -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.WatchOnlySinglesig(
                    SetupArgs(
                        isRestoreFlow = true,
                        isWatchOnly = true,
                        isSinglesig = true,
                    )
                )))
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

        doAsync({
            val biometricsCipherProvider = viewModelScope.async(
                start = CoroutineStart.LAZY
            ) {
                CompletableDeferred<PlatformCipher>().let {
                    biometricsPlatformCipher = it
                    postSideEffect(SideEffects.RequestBiometricsCipher)
                    it.await()
                }
            }

            onProgressDescription.value = getString(Res.string.id_creating_wallet)

            try {
                val cipher = if (greenKeystore.canUseBiometrics()) {
                    biometricsCipherProvider.await()
                } else return@doAsync null

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
        }, preAction = {
            onProgress.value = true
        }, postAction = {
            onProgress.value = false
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
