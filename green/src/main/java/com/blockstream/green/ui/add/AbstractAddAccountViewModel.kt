package com.blockstream.green.ui.add

import androidx.lifecycle.MutableLiveData
import com.blockstream.gdk.SATOSHI_UNIT
import com.blockstream.gdk.data.Account
import com.blockstream.gdk.data.AccountType
import com.blockstream.gdk.data.Network
import com.blockstream.gdk.params.SubAccountParams
import com.blockstream.green.data.Countly
import com.blockstream.green.database.CredentialType
import com.blockstream.green.database.LoginCredentials
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.devices.DeviceResolver
import com.blockstream.green.gdk.hasHistory
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.nameCleanup

open class AbstractAddAccountViewModel constructor(
    val appKeystore: AppKeystore,
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    wallet: Wallet,
) : AbstractWalletViewModel(sessionManager, walletRepository, countly, wallet) {

    val accountCreated = MutableLiveData<Account>()

    val accountTypeBeingCreated = MutableLiveData<AccountType>()

    fun createAccount(accountType: AccountType, accountName: String, network: Network, mnemonic: String?, xpub: String?) {
        accountTypeBeingCreated.value = accountType

        doUserAction({
            if(accountType.isLightning()){
                val isEmptyWallet = session.accounts.isEmpty()
                session.initLightningIfNeeded()

                // Persist Lightning
                session.lightningSdk.appGreenlightCredentials?.also {
                    val encryptedData = appKeystore.encryptData(it.toJson().toByteArray())

                    val loginCredentials = LoginCredentials(
                        walletId = wallet.id,
                        network = network.id,
                        credentialType = CredentialType.KEYSTORE_GREENLIGHT_CREDENTIALS,
                        encryptedData = encryptedData
                    )

                    walletRepository.insertOrReplaceLoginCredentials(loginCredentials)
                }

                // If wallet is new and LN is created, default to Satoshi
                if (isEmptyWallet) {
                    session.getSettings()?.also {
                        session.changeGlobalSettings(it.copy(unit = SATOSHI_UNIT))
                        session.updateSettings()
                    }
                }

                return@doUserAction session.lightningAccount
            }

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