package com.blockstream.green.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blockstream.gdk.data.Account
import com.blockstream.gdk.data.AccountAsset
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.managers.SessionManager
import mu.KLogging


abstract class AbstractAssetWalletViewModel constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    wallet: Wallet,
    initAccountAsset: AccountAsset,
) : AbstractAccountWalletViewModel(sessionManager, walletRepository, countly, wallet, initAccountAsset.account) {

    protected var _accountAssetLiveData = MutableLiveData<AccountAsset>()
    val accountAssetLiveData: LiveData<AccountAsset>
        get() = _accountAssetLiveData

    var accountAsset
        get() = _accountAssetLiveData.value!!
        set(value) {
            _accountAssetLiveData.value = value
            setAccount(value.account)
        }

    // Override this, as accountAsset may not be updated in time
    override val account: Account
        get() = accountAsset.account

    protected open val filterSubAccountsWithBalance = false

    init {
        _accountAssetLiveData.value = initAccountAsset
    }

    companion object : KLogging()
}