package com.blockstream.green.ui.add

import androidx.lifecycle.MutableLiveData
import com.blockstream.gdk.data.Account
import com.blockstream.gdk.data.AccountType
import com.blockstream.gdk.data.Network
import com.blockstream.gdk.params.SubAccountParams
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.devices.DeviceResolver
import com.blockstream.green.gdk.hasHistory
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.nameCleanup

open class AbstractAddAccountViewModel constructor(
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    wallet: Wallet,
) : AbstractWalletViewModel(sessionManager, walletRepository, countly, wallet) {

    val accountCreated = MutableLiveData<Account>()

    fun createAccount(accountType: AccountType, accountName: String, network: Network, mnemonic: String?, xpub: String?) {

        doUserAction({
            // Check if network needs initialization
            if(!session.hasActiveNetwork(network) && !session.failedNetworksFlow.value.contains(network)){
                session.initNetworkIfNeeded(network) { }
            }

            val accountsWithSameType = session.allAccounts.filter { it.type == accountType && it.network == network }.size

            val name = (accountName.nameCleanup() ?: accountType.gdkType).let { name ->
                "$name ${(accountsWithSameType + 1).takeIf { it > 1 } ?: ""}".trim()
            }

            val params = SubAccountParams(
                name = name,
                type = accountType,
                recoveryMnemonic = mnemonic,
                recoveryXpub = xpub,
            )

            // Find an archived account if exists, except if AccountType is 2of3 where a new key is used.
            val noHistoryArchivedAccount = if (accountType == AccountType.TWO_OF_THREE) {
                null
            } else {
                session.allAccountsFlow.value.find {
                    it.hidden && it.network == network && it.type == accountType && !it.hasHistory(
                        session
                    )
                }
            }

            // Check if account unarchive is needed
            if (noHistoryArchivedAccount != null) {
                session.updateAccount(noHistoryArchivedAccount, false)
            }else {
                session.createAccount(
                    network = network,
                    params = params,
                    hardwareWalletResolver = DeviceResolver(session.hwWallet, this)
                )
            }
        }, postAction = {
            onProgress.value = it == null
        }, onSuccess = {
            accountCreated.value = it
            countly.createAccount(session, it)
        })
    }
}