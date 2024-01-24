package com.blockstream.green.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import mu.KLogging


abstract class AbstractAccountWalletViewModel constructor(
    wallet: GreenWallet,
    initAccountAsset: AccountAsset
) : AbstractWalletViewModel(wallet, accountAssetOrNull = initAccountAsset) {

    val network
        get() = account.network

    private val _accountLiveData: LiveData<Account> = this.accountAsset.filterNotNull().map { it.account }.asLiveData()

    open val accountValue: Account
        get() = _accountLiveData.value!!
    val accountLiveData: LiveData<Account> get() = _accountLiveData

    open fun setAccount(account: Account) {
        accountAsset.value = AccountAsset.fromAccount(account)
    }

    override fun renameAccount(account: Account, name: String, callback: ((Account) -> Unit)?) {
        super.renameAccount(account, name){ updatedAccount ->
            if(account.id == updatedAccount.id) {
                accountAsset.value = AccountAsset.fromAccount(account)
            }
        }
    }

    override fun updateAccountVisibility(account: Account, isHidden: Boolean, callback: ((Account) -> Unit)?) {
        super.updateAccountVisibility(account, isHidden) { updatedAccount ->
            if(account.id == updatedAccount.id) {
                accountAsset.value = AccountAsset.fromAccount(account)
            }

            callback?.invoke(updatedAccount)
        }
    }

    companion object : KLogging()
}