package com.blockstream.common.models.recovery

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.essenty.statekeeper.StateKeeper
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.arkivanov.essenty.statekeeper.consume
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.navigation.NavigateDestinations
import com.rickclephas.kmm.viewmodel.MutableStateFlow
import com.rickclephas.kmm.viewmodel.coroutineScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    private val state: State = stateKeeper.consume(STATE) ?: State(
        mnemonic = gdk.generateMnemonic12(), mnemonicSize = 12
    )

    override val mnemonic = MutableStateFlow(viewModelScope, state.mnemonic)

    override val mnemonicSize = MutableStateFlow(viewModelScope, state.mnemonicSize)

    class LocalSideEffects{
        class LaunchUserPresence(val navigateTo: SideEffects.SideEffectEvent) : SideEffect
    }

    init {
        stateKeeper.register(STATE) {
            State(mnemonic = mnemonic.value, mnemonicSize = mnemonicSize.value)
        }

        if (setupArgs.isGenerateMnemonic) {
            mnemonicSize.drop(1).onEach {
                mnemonic.value =
                    if (it == 12) gdk.generateMnemonic12() else gdk.generateMnemonic24()
            }.launchIn(viewModelScope.coroutineScope)
        }

        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if(event is Events.Continue) {
            if (setupArgs.isShowRecovery) {
                val navigateTo = SideEffects.NavigateTo(
                    NavigateDestinations.RecoveryPhrase(
                        nextRecoveryArgs()
                    )
                )

                if(greenKeystore.canUseBiometrics()){
                    postSideEffect(
                        LocalSideEffects.LaunchUserPresence(
                            SideEffects.SideEffectEvent(
                                Events.EventSideEffect(navigateTo)
                            )
                        )
                    )
                }else {
                    postSideEffect(
                        navigateTo
                    )
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
        }
    }

    private fun nextRecoveryArgs(): SetupArgs {
        return if(setupArgs.isGenerateMnemonic){
            setupArgs.copy(mnemonic = mnemonic.value)
        }else{
            setupArgs
        }
    }

    @Parcelize
    private class State(
        val mnemonic: String, val mnemonicSize: Int
    ) : Parcelable

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