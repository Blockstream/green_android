package com.blockstream.green.ui.swap

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.SwapProposal
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import mu.KLogging


abstract class AbstractSwapWalletViewModel constructor(
    wallet: GreenWallet,
    proposal: SwapProposal?
) : AbstractWalletViewModel(wallet) {

    open val proposal: SwapProposal?
        get() = _proposalLiveData.value

    private val _proposalLiveData: MutableLiveData<SwapProposal> = MutableLiveData(proposal)
    val proposalLiveData: LiveData<SwapProposal> get() = _proposalLiveData

    companion object : KLogging()
}