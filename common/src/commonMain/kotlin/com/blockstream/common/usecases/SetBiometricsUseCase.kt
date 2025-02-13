package com.blockstream.common.usecases

import com.blockstream.common.crypto.GreenKeystore
import com.blockstream.common.crypto.PlatformCipher
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.database.Database
import com.blockstream.common.extensions.createLoginCredentials
import com.blockstream.common.gdk.GdkSession

class SetBiometricsUseCase(
    private val database: Database,
    private val greenKeystore: GreenKeystore,
) {

    suspend operator fun invoke(
        session: GdkSession,
        cipher: PlatformCipher,
        wallet: GreenWallet
    ) {
        session.getCredentials().mnemonic?.also { mnemonic ->
            val encryptedData = greenKeystore.encryptData(cipher, mnemonic.encodeToByteArray())

            database.replaceLoginCredential(
                createLoginCredentials(
                    walletId = wallet.id,
                    network = session.defaultNetwork.id,
                    credentialType = CredentialType.BIOMETRICS_MNEMONIC,
                    encryptedData = encryptedData // the Mnemonic encrypted with Cipher
                )
            )
        }
    }
}