package com.blockstream.green.ui.swap

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blockstream.gdk.data.SwapProposal
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import mu.KLogging


abstract class AbstractSwapWalletViewModel constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    wallet: Wallet,
    proposal: SwapProposal?
) : AbstractWalletViewModel(sessionManager, walletRepository, countly, wallet) {

    open val proposal: SwapProposal?
        get() = _proposalLiveData.value

    private val _proposalLiveData: MutableLiveData<SwapProposal> = MutableLiveData(proposal)
    val proposalLiveData: LiveData<SwapProposal> get() = _proposalLiveData

    companion object : KLogging()
}