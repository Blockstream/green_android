package com.blockstream.green.database

import androidx.room.Embedded
import androidx.room.Relation

data class WalletAndLoginCredentials(
    @Embedded
    val wallet: Wallet,
    @Relation(
        parentColumn = "id",
        entityColumn = "wallet_id"
    )
    val loginCredentials: List<LoginCredentials>
) {
    val pinPinData
        get() = loginCredentials.find { it.credentialType == CredentialType.PIN_PINDATA && it.counter < 3 }
    val biometricsPinData
        get() = loginCredentials.find { it.credentialType == CredentialType.BIOMETRICS_PINDATA }
    val passwordPinData
        get() = loginCredentials.find { it.credentialType == CredentialType.PASSWORD_PINDATA }

    val lightningCredentials
        get() = loginCredentials.find { it.credentialType == CredentialType.KEYSTORE_GREENLIGHT_CREDENTIALS }

    val watchOnlyCredentials
        get() = loginCredentials.find {
            it.credentialType == CredentialType.KEYSTORE_PASSWORD || // Deprecated
            it.credentialType == CredentialType.KEYSTORE_WATCHONLY_CREDENTIALS ||
            it.credentialType == CredentialType.BIOMETRICS_WATCHONLY_CREDENTIALS
        }

    val biometricsWatchOnlyCredentials
        get() = loginCredentials.find { it.credentialType == CredentialType.BIOMETRICS_WATCHONLY_CREDENTIALS }

}