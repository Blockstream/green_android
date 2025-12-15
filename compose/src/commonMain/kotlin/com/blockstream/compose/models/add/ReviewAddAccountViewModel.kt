package com.blockstream.compose.models.add

import androidx.lifecycle.viewModelScope
import com.blockstream.data.data.SetupArgs
import com.blockstream.compose.extensions.previewNetwork
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.navigation.NavData
import kotlinx.coroutines.launch

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


