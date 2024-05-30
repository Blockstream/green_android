package com.blockstream.common.models.recovery

import com.arkivanov.essenty.statekeeper.StateKeeper
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.blockstream.common.data.NavData
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.Serializable
import org.koin.core.component.inject

abstract class RecoveryIntroViewModelAbstract(val setupArgs: SetupArgs) :
    GreenViewModel(greenWalletOrNull = setupArgs.greenWallet) {
    override fun screenName(): String = "RecoveryIntro"

    @NativeCoroutinesState
    abstract val mnemonicSize: MutableStateFlow<Int>

    @NativeCoroutinesState
    abstract val mnemonic: MutableStateFlow<String>
}

class RecoveryIntroViewModel constructor(setupArgs: SetupArgs, stateKeeper: StateKeeper = StateKeeperDispatcher()) :
    RecoveryIntroViewModelAbstract(setupArgs = setupArgs) {
    private val gdk: Gdk by inject()

    private val state: State = stateKeeper.consume(STATE, State.serializer()) ?: State(
        mnemonic = gdk.generateMnemonic12(), mnemonicSize = 12
    )

    override val mnemonic = MutableStateFlow(viewModelScope, state.mnemonic)

    override val mnemonicSize = MutableStateFlow(viewModelScope, state.mnemonicSize)

    class LocalSideEffects{
        object LaunchUserPresence : SideEffect
    }

    class LocalEvents {
        class Authenticated(val authenticated: Boolean): Event
    }

    init {
        stateKeeper.register(STATE, State.serializer()) {
            State(mnemonic = mnemonic.value, mnemonicSize = mnemonicSize.value)
        }

        if (setupArgs.isGenerateMnemonic) {
            mnemonicSize.drop(1).onEach {
                mnemonic.value =
                    if (it == 12) gdk.generateMnemonic12() else gdk.generateMnemonic24()
            }.launchIn(viewModelScope.coroutineScope)
        }

        _navData.value = NavData(title = "id_before_you_backup")

        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if(event is Events.Continue) {
            if (setupArgs.isShowRecovery) {
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
        } else if (event is LocalEvents.Authenticated){
            navigateToRecoveryPhrase()
        }
    }

    private fun navigateToRecoveryPhrase(){
        postSideEffect(
            SideEffects.NavigateTo(
                NavigateDestinations.RecoveryPhrase(
                    nextRecoveryArgs()
                )
            )
        )
    }

    private fun nextRecoveryArgs(): SetupArgs {
        return if(setupArgs.isGenerateMnemonic){
            setupArgs.copy(mnemonic = mnemonic.value)
        }else{
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

class RecoveryIntroViewModelPreview(setupArgs: SetupArgs) : RecoveryIntroViewModelAbstract(setupArgs = setupArgs) {
    override val mnemonicSize: MutableStateFlow<Int> = MutableStateFlow(viewModelScope, 1)
    override val mnemonic: MutableStateFlow<String> = MutableStateFlow(viewModelScope, "chalk verb patch cube sell west penalty fish park worry tribe tourist")

    companion object {
        fun preview() = RecoveryIntroViewModelPreview(SetupArgs(mnemonic = ""))
    }
}