package com.blockstream.common.usecases

import com.blockstream.common.CountlyBase
import com.blockstream.common.SATOSHI_UNIT
import com.blockstream.common.crypto.GreenKeystore
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.toLoginCredentials
import com.blockstream.common.database.Database
import com.blockstream.common.extensions.cleanup
import com.blockstream.common.extensions.createLoginCredentials
import com.blockstream.common.extensions.hasHistory
import com.blockstream.common.extensions.richWatchOnly
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.device.DeviceResolver
import com.blockstream.common.gdk.device.HardwareWalletInteraction
import com.blockstream.common.gdk.params.SubAccountParams
import com.blockstream.green.utils.Loggable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class CreateAccountUseCase(
    val database: Database,
    val greenKeystore: GreenKeystore,
    val countly: CountlyBase
) : Loggable() {

    suspend operator fun invoke(
        session: GdkSession,
        greenWallet: GreenWallet,
        accountType: AccountType,
        network: Network,
        accountName: String? = null,
        mnemonic: String? = null,
        xpub: String? = null,
        hwInteraction: HardwareWalletInteraction? = null
    ): Account {
        return (if (accountType.isLightning()) {
            val isEmptyWallet = session.accounts.value.isEmpty()

            session.initLightningIfNeeded(mnemonic = mnemonic)

            if (!greenWallet.isEphemeral && !session.isHardwareWallet) {
                // Persist Lightning
                session.lightningSdk.appGreenlightCredentials?.also {
                    val encryptedData =
                        greenKeystore.encryptData(it.toJson().encodeToByteArray())

                    val loginCredentials = createLoginCredentials(
                        walletId = greenWallet.id,
                        network = network.id,
                        credentialType = CredentialType.KEYSTORE_GREENLIGHT_CREDENTIALS,
                        encryptedData = encryptedData
                    )

                    database.replaceLoginCredential(loginCredentials)
                }
            }

            // Save Lightning mnemonic
            if (!greenWallet.isEphemeral) {
                val encryptedData = withContext(context = Dispatchers.IO) {
                    (mnemonic ?: session.deriveLightningMnemonic()).let {
                        greenKeystore.encryptData(it.encodeToByteArray())
                    }
                }

                database.replaceLoginCredential(
                    createLoginCredentials(
                        walletId = greenWallet.id,
                        network = network.id,
                        credentialType = CredentialType.LIGHTNING_MNEMONIC,
                        encryptedData = encryptedData
                    )
                )
            }

//                if (appInfo.isDevelopmentOrDebug) {
//                    logger.i { "Development/Debug feature setCloseToAddress" }
//                    session.accounts.value.filter { it.isBitcoin }.let { accounts ->
//                        accounts.find { it.type == AccountType.BIP84_SEGWIT }
//                            ?: accounts.find { it.type == AccountType.BIP49_SEGWIT_WRAPPED }
//                    }?.also {
//                        session.lightningSdk.setCloseToAddress(session.getReceiveAddress(it).address)
//                    }
//                }

            // If wallet is new and LN is created, default to Satoshi
            if (isEmptyWallet) {
                session.getSettings()?.also {
                    session.changeGlobalSettings(it.copy(unit = SATOSHI_UNIT))
                }
            }

            return session.lightningAccount
        } else {

            // Check if network needs initialization
            if (!session.hasActiveNetwork(network) && !session.failedNetworks.value.contains(network)) {
                session.initNetworkIfNeeded(
                    network = network,
                    hardwareWalletResolver = DeviceResolver.createIfNeeded(
                        session.gdkHwWallet,
                        hwInteraction
                    )
                ) { }

                // Update rich watch only credentials if needed
                database.getLoginCredential(greenWallet.id, CredentialType.RICH_WATCH_ONLY)
                    ?.richWatchOnly(greenKeystore)?.also {
                        session.updateRichWatchOnly(it).toLoginCredentials(
                            session = session,
                            greenWallet = greenWallet,
                            greenKeystore = greenKeystore
                        ).also {
                            database.replaceLoginCredential(it)
                        }
                    }
            }

            val accountsWithSameType =
                session.allAccounts.value.filter { it.type == accountType && it.network == network }.size

            val name = (accountName.cleanup() ?: accountType.toString()).let { name ->
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
            } else {
                session.createAccount(
                    network = network,
                    params = params,
                    hardwareWalletResolver = DeviceResolver.createIfNeeded(
                        session.gdkHwWallet,
                        hwInteraction
                    )
                )
            }
        }).also {
            countly.createAccount(session, it)
        }
    }
}