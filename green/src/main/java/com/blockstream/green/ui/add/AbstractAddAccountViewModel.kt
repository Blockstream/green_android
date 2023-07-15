package com.blockstream.green.ui.add

import androidx.lifecycle.MutableLiveData
import com.blockstream.common.SATOSHI_UNIT
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.cleanup
import com.blockstream.common.extensions.createLoginCredentials
import com.blockstream.common.extensions.hasHistory
import com.blockstream.common.extensions.logException
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.device.DeviceResolver
import com.blockstream.common.gdk.params.SubAccountParams
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import kotlinx.coroutines.launch

open class AbstractAddAccountViewModel constructor(
    wallet: GreenWallet,
) : AbstractWalletViewModel(wallet) {

    val accountCreated = MutableLiveData<Account>()

    val accountTypeBeingCreated = MutableLiveData<AccountType>()

    fun createAccount(accountType: AccountType, accountName: String, network: Network, mnemonic: String?, xpub: String?) {
        accountTypeBeingCreated.value = accountType

        doUserAction({
            if(accountType.isLightning()){
                val isEmptyWallet = session.accounts.value.isEmpty()
                session.initLightningIfNeeded()

                if (!wallet.isEphemeral) {
                    // Persist Lightning
                    session.lightningSdk.appGreenlightCredentials?.also {
                        val encryptedData = greenKeystore.encryptData(it.toJson().toByteArray())

                        val loginCredentials = createLoginCredentials(
                            walletId = wallet.id,
                            credentialType = CredentialType.KEYSTORE_GREENLIGHT_CREDENTIALS,
                            network = network.id,
                            encryptedData = encryptedData
                        )

                        database.replaceLoginCredential(loginCredentials)
                    }
                }

                // If wallet is new and LN is created, default to Satoshi
                if (isEmptyWallet) {
                    session.getSettings()?.also {
                        session.changeGlobalSettings(it.copy(unit = SATOSHI_UNIT))
                    }
                }

                return@doUserAction session.lightningAccount
            }

            // Check if network needs initialization
            if(!session.hasActiveNetwork(network) && !session.failedNetworks.value.contains(network)){
                session.initNetworkIfNeeded(network) { }
            }

            val accountsWithSameType = session.allAccounts.value.filter { it.type == accountType && it.network == network }.size

            val name = (accountName.cleanup() ?: accountType.gdkType).let { name ->
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
                session.allAccounts.value.find {
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
                    hardwareWalletResolver = DeviceResolver.createIfNeeded(session.gdkHwWallet, this)
                )
            }
        }, postAction = {
            onProgressAndroid.value = it == null
        }, onSuccess = {
            accountCreated.value = it
            countly.createAccount(session, it)
        })
    }

    fun enableLightningShortcut() {
        applicationScope.launch(context = logException(countly)) {
            _enableLightningShortcut()
        }
    }
}