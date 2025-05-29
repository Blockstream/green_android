package com.blockstream.common.models.onboarding.phone

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_creating_wallet
import blockstream_green.common.generated.resources.id_recovery_phrase_check
import blockstream_green.common.generated.resources.id_restoring_your_wallet
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.usecases.CheckRecoveryPhraseUseCase
import com.blockstream.common.usecases.NewWalletUseCase
import com.blockstream.common.usecases.RestoreWalletUseCase
import com.blockstream.green.utils.Loggable
import com.blockstream.ui.events.Event
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

abstract class PinViewModelAbstract(
    val setupArgs: SetupArgs
) : GreenViewModel(greenWalletOrNull = setupArgs.greenWallet) {
    override fun screenName(): String = "OnBoardPin"

    override fun segmentation(): HashMap<String, Any>? = setupArgs.let { countly.onBoardingSegmentation(setupArgs = it) }

    @NativeCoroutinesState
    abstract val rocketAnimation: StateFlow<Boolean>
}

class PinViewModel constructor(
    setupArgs: SetupArgs
) : PinViewModelAbstract(setupArgs) {

    private val newWalletUseCase: NewWalletUseCase by inject()
    private val restoreWalletUseCase: RestoreWalletUseCase by inject()
    private val checkRecoveryPhraseUseCase: CheckRecoveryPhraseUseCase by inject()

    @NativeCoroutinesState
    override val rocketAnimation: MutableStateFlow<Boolean> =
        MutableStateFlow(viewModelScope, false)

    class LocalEvents {
        class SetPin(val pin: String) : Event
    }

    override val isLoginRequired: Boolean
        get() = false

    init {
        if (setupArgs.isRestoreFlow) {
            checkRecoveryPhrase(
                setupArgs = setupArgs
            )
        }

        onProgress.onEach {
            _navData.value = _navData.value.copy(isVisible = !it)
        }.launchIn(this)

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.SetPin) {

            if (event.pin.length == 6) {
                if (setupArgs.isRestoreFlow) {
                    restoreWallet(
                        setupArgs = setupArgs,
                        pin = event.pin,
                    )
                } else {
                    createNewWallet(
                        setupArgs = setupArgs, pin = event.pin
                    )
                }
            } else {
                postSideEffect(SideEffects.ErrorDialog(Exception("PIN should be 6 digits")))
            }
        } else if (event is Events.Continue) {
            _greenWallet?.also {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.WalletOverview(greenWallet = it, showWalletOnboarding = true)))
            }
        }
    }

    private fun checkRecoveryPhrase(setupArgs: SetupArgs) {
        doAsync({
            onProgressDescription.value = getString(Res.string.id_recovery_phrase_check)

            checkRecoveryPhraseUseCase.invoke(
                session = session,
                isTestnet = setupArgs.isTestnet == true,
                mnemonic = setupArgs.mnemonic,
                password = setupArgs.password
            )

        }, onSuccess = {

        }, onError = {
            if (it.message?.startsWith("id_wallet_already_restored") == true || it.message?.startsWith("id_the_recovery_phrase_doesnt") == true) {
                postSideEffect(SideEffects.NavigateBack(error = it))
            } else if (it.message == "id_login_failed") {
                postSideEffect(SideEffects.NavigateBack(error = Exception("id_no_multisig_shield_wallet")))
            } else if (it.message?.lowercase()?.contains("decrypt_mnemonic") == true || it.message?.lowercase()
                    ?.contains("invalid checksum") == true
            ) {
                postSideEffect(SideEffects.NavigateBack(error = Exception("id_error_passphrases_do_not_match")))
            } else {
                postSideEffect(SideEffects.ErrorDialog(it))
            }
        })
    }

    private fun createNewWallet(setupArgs: SetupArgs, pin: String) {
        doAsync({
            onProgressDescription.value = getString(Res.string.id_creating_wallet)

            newWalletUseCase.invoke(
                session = session,
                pin = pin,
                isTestnet = setupArgs.isTestnet == true
            )

        }, preAction = {
            onProgress.value = true
            rocketAnimation.value = true
        }, postAction = {
            onProgress.value = it == null
            rocketAnimation.value = it == null
        }, onSuccess = {
            postSideEffect(SideEffects.NavigateTo(NavigateDestinations.WalletOverview(it)))
        })
    }

    private fun restoreWallet(
        setupArgs: SetupArgs,
        pin: String,
    ) {
        doAsync({
            onProgressDescription.value = getString(Res.string.id_restoring_your_wallet)

            restoreWalletUseCase.invoke(
                session = session,
                setupArgs = setupArgs,
                pin = pin,
                greenWallet = greenWalletOrNull,
                cipher = null
            )

        }, preAction = {
            onProgress.value = true
            rocketAnimation.value = true
        }, postAction = {
            onProgress.value = it == null
            rocketAnimation.value = it == null
        }, onSuccess = {
            postSideEffect(SideEffects.NavigateTo(NavigateDestinations.WalletOverview(it)))
        })
    }

    companion object : Loggable()
}

class PinViewModelPreview(setupArgs: SetupArgs) : PinViewModelAbstract(setupArgs) {

    override val rocketAnimation: MutableStateFlow<Boolean>
        get() = MutableStateFlow(viewModelScope, false)

    companion object {
        fun preview() = PinViewModelPreview(SetupArgs(mnemonic = "neutral inherit learn"))
    }
}