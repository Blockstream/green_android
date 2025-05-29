package com.blockstream.common.models.add

import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.previewNetwork
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.NavData
import com.rickclephas.kmp.observableviewmodel.launch

abstract class ReviewAddAccountViewModelAbstract(
    val setupArgs: SetupArgs
) : AddAccountViewModelAbstract(
    greenWallet = setupArgs.greenWallet!!,
    assetId = setupArgs.assetId,
    popTo = setupArgs.popTo
) {
    override fun screenName(): String = "AddAccountConfirm"
}

class ReviewAddAccountViewModel(setupArgs: SetupArgs) : ReviewAddAccountViewModelAbstract(setupArgs = setupArgs) {
    init {
        viewModelScope.launch {
            _navData.value = NavData(title = setupArgs.accountType?.toString(), subtitle = greenWallet.name)
        }

        _isValid.value = true

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)
        if (event is Events.Continue) {
            createAccount(
                accountType = setupArgs.accountType!!,
                accountName = setupArgs.accountType.toString(),
                network = setupArgs.network!!,
                mnemonic = setupArgs.mnemonic,
                xpub = setupArgs.xpub
            )
        }
    }
}

class ReviewAddAccountViewModelPreview(setupArgs: SetupArgs) : ReviewAddAccountViewModelAbstract(setupArgs = setupArgs) {
    companion object {
        fun preview() = ReviewAddAccountViewModelPreview(
            setupArgs = SetupArgs(
                greenWallet = previewWallet(isHardware = true),
                network = previewNetwork(),
                accountType = AccountType.TWO_OF_THREE,
                xpub = "xpub",
            )
        )
    }
}


