package com.blockstream.data.usecases

import com.blockstream.data.data.GreenWallet
import com.blockstream.data.database.Database
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.params.LoginCredentialsParams

class CheckRecoveryPhraseUseCase(
    private val database: Database,
) {

    suspend operator fun invoke(
        session: GdkSession,
        isTestnet: Boolean,
        mnemonic: String,
        password: String?,
        greenWallet: GreenWallet? = null,
    ) {

        session.loginWithMnemonic(
            isTestnet = isTestnet,
            loginCredentialsParams = LoginCredentialsParams(
                mnemonic = mnemonic, password = password
            ),
            initNetworks = listOf(session.prominentNetwork(isTestnet)),
            initializeSession = false,
            isSmartDiscovery = false,
            isCreate = true,
            isRestore = false
        ).also {
            if (greenWallet == null) {
                // Check if wallet already exists
                it.xpubHashId.also { walletHashId ->
                    database.getWalletWithXpubHashId(
                        xPubHashId = walletHashId, isTestnet = isTestnet, isHardware = false
                    )?.also { wallet ->
                        throw Exception("id_wallet_already_restored_s|${wallet.name}")
                    }
                }
            } else {
                // check if walletHashId is the same (also use networkHashId for backwards compatibility)
                if (greenWallet.xPubHashId.isNotBlank() && (greenWallet.xPubHashId != it.xpubHashId && greenWallet.xPubHashId != it.networkHashId)) {
                    throw Exception("id_the_recovery_phrase_doesnt")
                }
            }
        }
    }
}