package com.blockstream.domain.account

import com.blockstream.data.CountlyBase
import com.blockstream.data.SATOSHI_UNIT
import com.blockstream.data.crypto.GreenKeystore
import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.toLoginCredentials
import com.blockstream.data.database.Database
import com.blockstream.data.extensions.cleanup
import com.blockstream.data.extensions.hasHistory
import com.blockstream.data.extensions.richWatchOnly
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.device.DeviceResolver
import com.blockstream.data.gdk.device.HardwareWalletInteraction
import com.blockstream.data.gdk.params.SubAccountParams
import com.blockstream.data.managers.WalletSettingsManager
import com.blockstream.domain.lightning.LightningNodeIdUseCase
import com.blockstream.domain.wallet.SaveDerivedLightningMnemonicUseCase
import com.blockstream.utils.Loggable

class CreateAccountUseCase(
    private val database: Database,
    private val greenKeystore: GreenKeystore,
    private val walletSettingsManager: WalletSettingsManager,
    private val countly: CountlyBase,
    private val lightningNodeIdUseCase: LightningNodeIdUseCase,
    private val saveDerivedLightningMnemonicUseCase: SaveDerivedLightningMnemonicUseCase
) : Loggable() {

    suspend operator fun invoke(
        session: GdkSession,
        wallet: GreenWallet,
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


            // Save Lightning mnemonic
            if (!wallet.isEphemeral) {
                walletSettingsManager.setLightningEnabled(walletId = wallet.id, true)

                saveDerivedLightningMnemonicUseCase.invoke(session = session, wallet = wallet, mnemonic = mnemonic)

                session.initLwkIfNeeded(wallet = wallet, mnemonic = mnemonic)
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

            // Save Lightning Node Id
            lightningNodeIdUseCase.invoke(wallet = wallet, session = session)

            return session.lightningAccount
        } else {

            // Check if network needs initialization
            if (!session.hasActiveNetwork(network) && !session.failedNetworks.value.contains(network)) {
                session.initNetworkIfNeeded(
                    network = network,
                    hardwareWalletResolver = DeviceResolver.Companion.createIfNeeded(
                        session.gdkHwWallet,
                        hwInteraction
                    )
                ) { }

                // Update rich watch only credentials if needed
                database.getLoginCredential(wallet.id, CredentialType.RICH_WATCH_ONLY)
                    ?.richWatchOnly(greenKeystore)?.also {
                        session.updateRichWatchOnly(it).toLoginCredentials(
                            session = session,
                            greenWallet = wallet,
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
                    hardwareWalletResolver = DeviceResolver.Companion.createIfNeeded(
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
