package com.blockstream.green.ui.wallet

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import mu.KLogging


abstract class AbstractAccountWalletViewModel(
    wallet: GreenWallet,
    initAccountAsset: AccountAsset
) : AbstractWalletViewModel(wallet, accountAssetOrNull = initAccountAsset) {

    val network
        get() = account.network

    open val accountValue: Account
        get() = accountAsset.value!!.account

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