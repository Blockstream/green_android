package com.blockstream.green.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blockstream.gdk.data.Account
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.managers.SessionManager
import mu.KLogging


abstract class AbstractAccountWalletViewModel constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    wallet: Wallet,
    account: Account
) : AbstractWalletViewModel(sessionManager, walletRepository, countly, wallet) {

    val network
        get() = account.network

    open val account: Account
        get() = _accountLiveData.value!!

    private val _accountLiveData: MutableLiveData<Account> = MutableLiveData(account)
    val accountLiveData: LiveData<Account> get() = _accountLiveData

    open fun setAccount(account: Account) {
        _accountLiveData.value = account
    }

    override fun renameAccount(account: Account, name: String, callback: ((Account) -> Unit)?) {
        super.renameAccount(account, name){ updatedAccount ->
            if(account.id == updatedAccount.id) {
                _accountLiveData.value = updatedAccount
            }
        }
    }

    override fun updateAccountVisibility(account: Account, isHidden: Boolean, callback: ((Account) -> Unit)?) {
        super.updateAccountVisibility(account, isHidden) { updatedAccount ->
            if(account.id == updatedAccount.id) {
                _accountLiveData.value = updatedAccount
            }

            callback?.invoke(updatedAccount)
        }
    }

    companion object : KLogging()
}