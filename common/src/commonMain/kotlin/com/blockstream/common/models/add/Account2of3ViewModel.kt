package com.blockstream.common.models.add

import com.blockstream.common.data.NavData
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Event
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.rickclephas.kmp.observableviewmodel.launch

abstract class Account2of3ViewModelAbstract(
    val setupArgs: SetupArgs
) : AddAccountViewModelAbstract(
    greenWallet = setupArgs.greenWallet!!,
    assetId = setupArgs.assetId,
    popTo = setupArgs.popTo
) {
    override fun screenName(): String = "AddAccountChooseRecovery"
}

class Account2of3ViewModel(setupArgs: SetupArgs) :
    Account2of3ViewModelAbstract(setupArgs = setupArgs) {

    class LocalEvents {
        object NewRecovery : Event
        object ExistingRecovery : Event
        object Xpub : Event
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.NewRecovery -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.RecoveryIntro(
                            setupArgs = setupArgs.copy(
                                mnemonic = ""
                            )
                        )
                    )
                )
            }

            is LocalEvents.ExistingRecovery -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.EnterRecoveryPhrase(
                            setupArgs = setupArgs
                        )
                    )
                )
            }

            is LocalEvents.Xpub -> {
                postSideEffect(SideEffects.NavigateTo(NavigateDestinations.Xpub(setupArgs = setupArgs)))
            }
        }
    }

    init {
        viewModelScope.launch {
            _navData.value = NavData(title = setupArgs.accountType?.toString(), subtitle = greenWallet.name)
        }

        bootstrap()
    }
}

class Account2of3ViewModelPreview(setupArgs: SetupArgs) :
    Account2of3ViewModelAbstract(setupArgs = setupArgs) {
    companion object {
        fun preview() = Account2of3ViewModelPreview(
            setupArgs = SetupArgs(greenWallet = previewWallet(isHardware = true))
        )
    }
}


