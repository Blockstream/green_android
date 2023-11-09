package com.blockstream.common.models.add

import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.previewWallet

abstract class ReviewAddAccountViewModelAbstract(val setupArgs: SetupArgs
) : AddAccountViewModelAbstract(greenWallet = setupArgs.greenWallet!!) {
    override fun screenName(): String = "AddAccountConfirm"
}

class ReviewAddAccountViewModel(setupArgs: SetupArgs) : ReviewAddAccountViewModelAbstract(setupArgs = setupArgs) {
    init {
        bootstrap()
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)
        if(event is Events.Continue){
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
            setupArgs = SetupArgs(greenWallet = previewWallet(isHardware = true))
        )
    }
}


