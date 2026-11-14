package com.blockstream.domain.wallet

import com.blockstream.data.CountlyBase
import com.blockstream.data.crypto.PlatformCipher
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.SetupArgs
import com.blockstream.data.database.Database
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.params.LoginCredentialsParams
import com.blockstream.data.managers.SessionManager
import com.blockstream.data.managers.SettingsManager
import com.blockstream.data.usecases.SetBiometricsUseCase
import com.blockstream.data.usecases.SetPinUseCase
import com.blockstream.data.utils.generateWalletName
import com.blockstream.domain.lightning.LightningNodeIdUseCase
import com.blockstream.domain.receive.GetReceiveAddressUseCase

class RestoreWalletUseCase(
    private val database: Database,
    private val countly: CountlyBase,
    private val sessionManager: SessionManager,
    private val settingsManager: SettingsManager,
    private val setPinUseCase: SetPinUseCase,
    private val setBiometricsUseCase: SetBiometricsUseCase,
    private val lightningNodeIdUseCase: LightningNodeIdUseCase,
    private val saveGreenlightMnemonicAndCredentialsUseCase: SaveGreenlightMnemonicAndCredentialsUseCase,
    private val saveDerivedBoltzMnemonicUseCase: SaveDerivedBoltzMnemonicUseCase,
    private val getReceiveAddressUseCase: GetReceiveAddressUseCase
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
                isTestnet = setupArgs.isTestnet == true,
            )

            database.insertWallet(wallet)

            if (session.hasLightning) {
                saveGreenlightMnemonicAndCredentialsUseCase.invoke(session = session, wallet = wallet)

                lightningNodeIdUseCase.invoke(wallet = wallet, session = session)
            }

            // Used in Swaps
            saveDerivedBoltzMnemonicUseCase.invoke(session = session, wallet = wallet)

            val bitcoinAddress = session.accounts.value.firstOrNull { it.isBitcoin }?.let {
                getReceiveAddressUseCase(session, it).address
            }

            val liquidAddress = session.accounts.value.firstOrNull { it.isLiquid }?.let {
                getReceiveAddressUseCase(session, it).address
            }

            session.initLwkIfNeeded(wallet = wallet, bitcoinAddress = bitcoinAddress, liquidAddress = liquidAddress)

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
