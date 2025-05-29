package com.blockstream.common.usecases

import com.blockstream.common.CountlyBase
import com.blockstream.common.crypto.GreenKeystore
import com.blockstream.common.crypto.PlatformCipher
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.database.Database
import com.blockstream.common.extensions.createLoginCredentials
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.params.LoginCredentialsParams
import com.blockstream.common.managers.SessionManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.utils.generateWalletName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class RestoreWalletUseCase(
    private val greenKeystore: GreenKeystore,
    private val database: Database,
    private val countly: CountlyBase,
    private val sessionManager: SessionManager,
    private val settingsManager: SettingsManager,
    private val setPinUseCase: SetPinUseCase,
    private val setBiometricsUseCase: SetBiometricsUseCase,
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
                session.lightningSdk.appGreenlightCredentials?.also { credentials ->
                    val encryptedData =
                        greenKeystore.encryptData(credentials.toJson().encodeToByteArray())

                    val loginCredentials = createLoginCredentials(
                        walletId = wallet.id,
                        network = session.lightning!!.id,
                        credentialType = CredentialType.KEYSTORE_GREENLIGHT_CREDENTIALS,
                        encryptedData = encryptedData
                    )

                    database.replaceLoginCredential(loginCredentials)
                }

                val encryptedData = withContext(context = Dispatchers.IO) {
                    greenKeystore.encryptData(session.deriveLightningMnemonic().encodeToByteArray())
                }

                database.replaceLoginCredential(
                    createLoginCredentials(
                        walletId = wallet.id,
                        network = session.lightning!!.id,
                        credentialType = CredentialType.LIGHTNING_MNEMONIC,
                        encryptedData = encryptedData
                    )
                )
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