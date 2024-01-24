package com.blockstream.green.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountAsset
import kotlinx.coroutines.flow.filterNotNull
import mu.KLogging


abstract class AbstractAssetWalletViewModel constructor(
    wallet: GreenWallet,
    initAccountAsset: AccountAsset,
) : AbstractAccountWalletViewModel(wallet, initAccountAsset) {

    protected val _accountAssetLiveData = accountAsset.filterNotNull().asLiveData()
    val accountAssetLiveData: LiveData<AccountAsset>
        get() = _accountAssetLiveData

    var accountAssetValue
        get() = _accountAssetLiveData.value!!
        set(value) {
            accountAsset.value = value
            setAccount(value.account)
        }

    // Override this, as accountAsset may not be updated in time
    override val accountValue: Account
        get() = accountAssetValue.account

    protected open val filterSubAccountsWithBalance = false

    companion object : KLogging()
}