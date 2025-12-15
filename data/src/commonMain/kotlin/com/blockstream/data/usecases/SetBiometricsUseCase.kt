package com.blockstream.data.usecases

import com.blockstream.data.crypto.GreenKeystore
import com.blockstream.data.crypto.PlatformCipher
import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.database.Database
import com.blockstream.data.extensions.createLoginCredentials
import com.blockstream.data.gdk.GdkSession

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