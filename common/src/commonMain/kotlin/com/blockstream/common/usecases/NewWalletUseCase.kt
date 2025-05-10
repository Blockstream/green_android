package com.blockstream.common.usecases

import com.blockstream.common.CountlyBase
import com.blockstream.common.crypto.PlatformCipher
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.database.Database
import com.blockstream.common.extensions.title
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.AccountType
import com.blockstream.common.gdk.params.LoginCredentialsParams
import com.blockstream.common.gdk.params.SubAccountParams
import com.blockstream.common.managers.SessionManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.utils.generateWalletName
import com.blockstream.green.utils.Loggable

class NewWalletUseCase(
    private val gdk: Gdk,
    private val database: Database,
    private val countly: CountlyBase,
    private val sessionManager: SessionManager,
    private val settingsManager: SettingsManager,
    private val setPinUseCase: SetPinUseCase,
    private val setBiometricsUseCase: SetBiometricsUseCase,
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

        // Archive all accounts on the newly created wallet
        session.accounts.value.forEach { account ->
            logger.d { "Archive ${account.name}" }
            session.updateAccount(
                account = account, isHidden = true, resetAccountName = account.type.title()
            )
        }

        // Create Singlesig account
        val accountType = AccountType.BIP84_SEGWIT

        // Bitcoin
        session.bitcoinSinglesig?.also {
            logger.d { "Creating ${it.name} account" }
            session.createAccount(
                network = it,
                params = SubAccountParams(
                    name = accountType.toString(),
                    type = accountType,
                )
            )
        }

        // Liquid
        session.liquidSinglesig?.also {
            logger.d { "Creating ${it.name} account" }
            session.createAccount(
                network = it,
                params = SubAccountParams(
                    name = accountType.toString(),
                    type = accountType,
                )
            )
        }

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
        } else if(pin != null){
            setPinUseCase.invoke(session = session, pin = pin, wallet = wallet, onPinData = insertWalletToDatabase)
        } else{
            throw Exception("Neither Cipher nor Pin provided for wallet security")
        }

        sessionManager.upgradeOnBoardingSessionToWallet(wallet)

        countly.createWallet(session)

        return wallet
    }

    companion object: Loggable()
}