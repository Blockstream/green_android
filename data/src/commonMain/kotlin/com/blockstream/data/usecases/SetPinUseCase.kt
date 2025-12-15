package com.blockstream.data.usecases

import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.database.Database
import com.blockstream.data.extensions.createLoginCredentials
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.params.EncryptWithPinParams

class SetPinUseCase(
    private val database: Database,
) {

    suspend operator fun invoke(
        session: GdkSession,
        pin: String,
        wallet: GreenWallet,
        onPinData: (suspend () -> Unit)? = null
    ): Unit {

        session
            .getCredentials().let { credentials ->
                session.encryptWithPin(
                    network = null,
                    encryptWithPinParams = EncryptWithPinParams(
                        pin = pin,
                        credentials = credentials
                    )
                )
            }.also { encryptWithPin ->
                onPinData?.invoke()

                // Replace PinData
                database.replaceLoginCredential(
                    createLoginCredentials(
                        walletId = wallet.id,
                        network = encryptWithPin.network.id,
                        credentialType = CredentialType.PIN_PINDATA,
                        pinData = encryptWithPin.pinData
                    )
                )

                // We only allow one credential type PIN / Password
                // Password comes from v2 and should be deleted when a user tries to change his
                // password to a pin
                database.deleteLoginCredentials(wallet.id, CredentialType.PASSWORD_PINDATA)
            }
    }
}