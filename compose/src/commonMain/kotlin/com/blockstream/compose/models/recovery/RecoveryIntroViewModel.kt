package com.blockstream.compose.models.recovery

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_before_you_back_up
import com.arkivanov.essenty.statekeeper.StateKeeper
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.gdk.Gdk
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffect
import com.blockstream.compose.sideeffects.SideEffects
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

abstract class RecoveryIntroViewModelAbstract(val setupArgs: SetupArgs) :
    GreenViewModel(greenWalletOrNull = setupArgs.greenWallet) {
    override fun screenName(): String = "RecoveryIntro"
    abstract val mnemonicSize: MutableStateFlow<Int>
    abstract val mnemonic: MutableStateFlow<String>
}

class RecoveryIntroViewModel(
    setupArgs: SetupArgs,
    stateKeeper: StateKeeper = StateKeeperDispatcher()
) :
    RecoveryIntroViewModelAbstract(setupArgs = setupArgs) {
    private val gdk: Gdk by inject()

    private val state: State = stateKeeper.consume(STATE, State.serializer()) ?: State(
        mnemonic = gdk.generateMnemonic12(), mnemonicSize = 12
    )
    override val mnemonic = MutableStateFlow(state.mnemonic)
    override val mnemonicSize = MutableStateFlow(state.mnemonicSize)

    class LocalSideEffects {
        object LaunchUserPresence : SideEffect
    }

    class LocalEvents {
        class Authenticated(val authenticated: Boolean) : Event
    }

    init {
        stateKeeper.register(STATE, State.serializer()) {
            State(mnemonic = mnemonic.value, mnemonicSize = mnemonicSize.value)
        }

        if (setupArgs.isGenerateMnemonic) {
            mnemonicSize.drop(1).onEach {
                mnemonic.value =
                    if (it == 12) gdk.generateMnemonic12() else gdk.generateMnemonic24()
            }.launchIn(viewModelScope)
        }

        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_before_you_back_up),
                subtitle = greenWalletOrNull?.name
            )
        }

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is Events.Continue) {
            proceed()
        } else if (event is LocalEvents.Authenticated) {
            navigateToRecoveryPhrase()
        }
    }

    private fun proceed() {
        doAsync({
            if (setupArgs.isShowRecovery && greenWallet.isRecoveryConfirmed) {
                if (greenKeystore.canUseBiometrics()) {
                    postSideEffect(
                        LocalSideEffects.LaunchUserPresence
                    )
                } else {
                    navigateToRecoveryPhrase()
                }
            } else {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.RecoveryWords(
                            nextRecoveryArgs()
                        )
                    )
                )
            }
        })
    }

    private suspend fun navigateToRecoveryPhrase() {
        postSideEffect(
            SideEffects.NavigateTo(
                NavigateDestinations.RecoveryPhrase(
                    nextRecoveryArgs()
                )
            )
        )
    }

    private suspend fun nextRecoveryArgs(): SetupArgs {
        return if (setupArgs.isGenerateMnemonic) {
            setupArgs.copy(mnemonic = mnemonic.value)
        } else if (setupArgs.greenWallet?.isRecoveryConfirmed == false) {
            setupArgs.copy(
                mnemonic = session.getCredentials().mnemonic
                    ?: throw Exception("Couldn't get the mnemonic")
            )
        } else {
            setupArgs
        }
    }

    @Serializable
    private class State(
        val mnemonic: String, val mnemonicSize: Int
    )

    companion object {
        const val STATE = "STATE"

        fun from(setupArgs: SetupArgs) = RecoveryIntroViewModel(setupArgs = setupArgs)
    }
}

class RecoveryIntroViewModelPreview(setupArgs: SetupArgs) :
    RecoveryIntroViewModelAbstract(setupArgs = setupArgs) {
    override val mnemonicSize: MutableStateFlow<Int> = MutableStateFlow(1)
    override val mnemonic: MutableStateFlow<String> = MutableStateFlow(
        "chalk verb patch cube sell west penalty fish park worry tribe tourist"
    )

    companion object {
        fun preview() = RecoveryIntroViewModelPreview(SetupArgs(mnemonic = ""))
    }
}