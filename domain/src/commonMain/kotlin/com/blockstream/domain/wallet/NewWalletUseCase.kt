package com.blockstream.domain.wallet

import com.blockstream.data.CountlyBase
import com.blockstream.data.crypto.PlatformCipher
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.database.Database
import com.blockstream.data.gdk.Gdk
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.params.LoginCredentialsParams
import com.blockstream.data.managers.SessionManager
import com.blockstream.data.managers.SettingsManager
import com.blockstream.data.usecases.SetBiometricsUseCase
import com.blockstream.data.usecases.SetPinUseCase
import com.blockstream.data.utils.generateWalletName
import com.blockstream.utils.Loggable

class NewWalletUseCase(
    private val gdk: Gdk,
    private val database: Database,
    private val countly: CountlyBase,
    private val sessionManager: SessionManager,
    private val settingsManager: SettingsManager,
    private val setPinUseCase: SetPinUseCase,
    private val setBiometricsUseCase: SetBiometricsUseCase,
    private val saveDerivedBoltzMnemonicUseCase: SaveDerivedBoltzMnemonicUseCase
) {

    suspend operator fun invoke(
        session: GdkSession,
        pin: String? = null,
        cipher: PlatformCipher? = null,
        isTestnet: Boolean = false
    ): GreenWallet {

        val mnemonic = gdk.generateMnemonic12()

        val loginData = session.loginWithMnemonic(
            isTestnet = isTestnet,
            loginCredentialsParams = LoginCredentialsParams(mnemonic = mnemonic),
            initializeSession = true,
            isSmartDiscovery = false,
            isCreate = true,
            isRestore = false
        )

        session.setupDefaultAccounts()

        val wallet = GreenWallet.createWallet(
            name = generateWalletName(settingsManager),
            xPubHashId = loginData.xpubHashId,
            activeNetwork = session.activeAccount.value?.networkId
                ?: session.defaultNetwork.network,
            activeAccount = session.activeAccount.value?.pointer ?: 0,
            isRecoveryConfirmed = false,
            isTestnet = session.defaultNetwork.isTestnet
        )

        val insertWalletToDatabase = suspend {
            database.insertWallet(wallet)
        }

        // Biometrics
        if (cipher != null) {
            insertWalletToDatabase()
            setBiometricsUseCase.invoke(session = session, cipher = cipher, wallet = wallet)
        } else if (pin != null) {
            setPinUseCase.invoke(session = session, pin = pin, wallet = wallet, onPinData = insertWalletToDatabase)
        } else {
            throw Exception("Neither Cipher nor Pin provided for wallet security")
        }

        // Used in Swaps
        saveDerivedBoltzMnemonicUseCase.invoke(session = session, wallet = wallet)

        session.initLwkIfNeeded(wallet = wallet)

        sessionManager.upgradeOnBoardingSessionToWallet(wallet)

        countly.createWallet(session)

        return wallet
    }

    companion object : Loggable()
}
