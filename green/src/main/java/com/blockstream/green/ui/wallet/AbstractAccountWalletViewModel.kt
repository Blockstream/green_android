package com.blockstream.green.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.Account
import mu.KLogging


abstract class AbstractAccountWalletViewModel constructor(
    wallet: GreenWallet,
    account: Account
) : AbstractWalletViewModel(wallet) {

    val network
        get() = accountValue.network

    open val accountValue: Account
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