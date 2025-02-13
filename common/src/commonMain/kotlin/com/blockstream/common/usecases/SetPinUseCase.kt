package com.blockstream.common.usecases

import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.database.Database
import com.blockstream.common.extensions.createLoginCredentials
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.params.EncryptWithPinParams

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