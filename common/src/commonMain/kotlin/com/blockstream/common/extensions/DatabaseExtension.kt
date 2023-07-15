package com.blockstream.common.extensions

import com.benasher44.uuid.Uuid
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.EncryptedData
import com.blockstream.common.database.LoginCredentials
import com.blockstream.common.gdk.data.PinData
import com.blockstream.common.utils.getSecureRandom
import kotlinx.datetime.Clock


fun createLoginCredentials(
    walletId: String,
    network: String,
    credentialType: CredentialType,
    pinData: PinData? = null,
    encryptedData: EncryptedData? = null
): LoginCredentials {
    return LoginCredentials(
        wallet_id = walletId,
        credential_type = credentialType,
        network = network,
        pin_data = pinData,
        keystore = null,
        encrypted_data = encryptedData,
        counter = 0L
    )
}

val List<LoginCredentials>.pinPinData
    get() = find { it.credential_type == CredentialType.PIN_PINDATA && it.counter < 3 }
val List<LoginCredentials>.biometricsPinData
    get() = find { it.credential_type == CredentialType.BIOMETRICS_PINDATA }
val List<LoginCredentials>.passwordPinData
    get() = find { it.credential_type == CredentialType.PASSWORD_PINDATA }

val List<LoginCredentials>.lightningCredentials
    get() = find { it.credential_type == CredentialType.KEYSTORE_GREENLIGHT_CREDENTIALS }

val List<LoginCredentials>.lightningMnemonic
    get() = find { it.credential_type == CredentialType.LIGHTNING_MNEMONIC }

val List<LoginCredentials>.watchOnlyCredentials
    get() = find {
        it.credential_type == CredentialType.KEYSTORE_PASSWORD || // Deprecated
                it.credential_type == CredentialType.KEYSTORE_WATCHONLY_CREDENTIALS ||
                it.credential_type == CredentialType.BIOMETRICS_WATCHONLY_CREDENTIALS
    }

val List<LoginCredentials>.biometricsWatchOnlyCredentials
    get() = find { it.credential_type == CredentialType.BIOMETRICS_WATCHONLY_CREDENTIALS }




// Time-based UUID similar to MongoDB ObjectId
// https://github.com/benasher44/uuid/issues/75
fun objectId(
    id: Long = Clock.System.now().toEpochMilliseconds()
): Uuid {
    val random = getSecureRandom().unsecureRandomLong()
    return Uuid(id, random)
}