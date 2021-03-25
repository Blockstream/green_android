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
    val pin
        get() = loginCredentials.find { it.credentialType == CredentialType.PIN }
    val biometrics
        get() = loginCredentials.find { it.credentialType == CredentialType.BIOMETRICS }
    val keystore
        get() = loginCredentials.find { it.credentialType == CredentialType.KEYSTORE }
    val password
        get() = loginCredentials.find { it.credentialType == CredentialType.PASSWORD }
}