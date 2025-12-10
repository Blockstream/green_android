package com.blockstream.domain.wallet

import com.blockstream.common.CountlyBase
import com.blockstream.common.crypto.PlatformCipher
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.database.Database
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.params.LoginCredentialsParams
import com.blockstream.common.managers.SessionManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.managers.WalletSettingsManager
import com.blockstream.common.usecases.SetBiometricsUseCase
import com.blockstream.common.usecases.SetPinUseCase
import com.blockstream.common.utils.generateWalletName
import com.blockstream.domain.lightning.LightningNodeIdUseCase

class RestoreWalletUseCase(
    private val database: Database,
    private val countly: CountlyBase,
    private val sessionManager: SessionManager,
    private val settingsManager: SettingsManager,
    private val walletSettingsManager: WalletSettingsManager,
    private val setPinUseCase: SetPinUseCase,
    private val setBiometricsUseCase: SetBiometricsUseCase,
    private val lightningNodeIdUseCase: LightningNodeIdUseCase,
    private val saveDerivedLightningMnemonicUseCase: SaveDerivedLightningMnemonicUseCase,
    private val saveDerivedBoltzMnemonicUseCase: SaveDerivedBoltzMnemonicUseCase
) {

    suspend operator fun invoke(
        session: GdkSession,
        setupArgs: SetupArgs,
        pin: String? = null,
        greenWallet: GreenWallet? = null,
        cipher: PlatformCipher? = null,
    ): GreenWallet {

        session.loginWithMnemonic(
            isTestnet = setupArgs.isTestnet == true,
            loginCredentialsParams = LoginCredentialsParams(
                mnemonic = setupArgs.mnemonic, password = setupArgs.password
            ),
            initializeSession = true,
            isSmartDiscovery = false,
            isCreate = false,
            isRestore = true
        )

        // Wait for setup to gets completed so that the active account is set
        session.setupDefaultAccounts().join()

        val wallet: GreenWallet

        if (greenWallet == null) {
            wallet = GreenWallet.createWallet(
                name = generateWalletName(settingsManager),
                xPubHashId = session.xPubHashId ?: "",
                activeNetwork = session.activeAccount.value?.networkId
                    ?: session.defaultNetwork.id,
                activeAccount = session.activeAccount.value?.pointer ?: 0,
                isTestnet = setupArgs.isTestnet == true,
            )

            database.insertWallet(wallet)

            if (session.hasLightning) {
                // Used in Lightning
                saveDerivedLightningMnemonicUseCase.invoke(session = session, wallet = wallet)
            }

            // Used in Swaps
            saveDerivedBoltzMnemonicUseCase.invoke(session = session, wallet = wallet)

            val liquidAddress = session.accounts.value.firstOrNull { it.isLiquid }?.let {
                session.getReceiveAddressAsString(it)
            }

            session.initLwkIfNeeded(wallet = wallet, restoreSwapsAddress = liquidAddress)

            if (session.hasLightning) {
                walletSettingsManager.setLightningEnabled(walletId = wallet.id, true)

                lightningNodeIdUseCase.invoke(wallet = wallet, session = session)
            }

            sessionManager.upgradeOnBoardingSessionToWallet(wallet)

            countly.importWallet(session)
        } else {
            wallet = greenWallet
        }

        // Biometrics
        if (cipher != null) {
            setBiometricsUseCase.invoke(session = session, cipher = cipher, wallet = wallet)
        } else if (pin != null) {
            setPinUseCase.invoke(session = session, pin = pin, wallet = wallet)
        } else {
            throw Exception("Neither Cipher nor Pin provided for wallet security")
        }

        return wallet
    }
}
