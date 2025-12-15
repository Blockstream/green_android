@file:OptIn(ExperimentalUuidApi::class)

package com.blockstream.data.extensions

import com.blockstream.data.crypto.GreenKeystore
import com.blockstream.data.data.CredentialType
import com.blockstream.data.data.EncryptedData
import com.blockstream.data.data.RichWatchOnly
import com.blockstream.data.data.toRichWatchOnly
import com.blockstream.data.database.wallet.LoginCredentials
import com.blockstream.data.gdk.data.PinData
import com.blockstream.data.utils.getSecureRandom
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
val List<LoginCredentials>.mnemonic
    get() = find { it.credential_type == CredentialType.KEYSTORE_MNEMONIC }
val List<LoginCredentials>.biometrics
    get() = find { it.credential_type == CredentialType.BIOMETRICS_PINDATA || it.credential_type == CredentialType.BIOMETRICS_MNEMONIC || it.credential_type == CredentialType.BIOMETRICS_WATCHONLY_CREDENTIALS }
val List<LoginCredentials>.biometricsPinData
    get() = find { it.credential_type == CredentialType.BIOMETRICS_PINDATA }
val List<LoginCredentials>.biometricsMnemonic
    get() = find { it.credential_type == CredentialType.BIOMETRICS_MNEMONIC }
val List<LoginCredentials>.passwordPinData
    get() = find { it.credential_type == CredentialType.PASSWORD_PINDATA }

val List<LoginCredentials>.lightningMnemonic
    get() = find { it.credential_type == CredentialType.LIGHTNING_MNEMONIC }

val List<LoginCredentials>.watchOnlyCredentials
    get() = find {
        it.credential_type == CredentialType.KEYSTORE_PASSWORD || // Deprecated
                it.credential_type == CredentialType.KEYSTORE_WATCHONLY_CREDENTIALS ||
                it.credential_type == CredentialType.BIOMETRICS_WATCHONLY_CREDENTIALS
    }

val List<LoginCredentials>.hwWatchOnlyCredentials
    get() = find {
        it.credential_type == CredentialType.KEYSTORE_HW_WATCHONLY_CREDENTIALS
    }

val List<LoginCredentials>.biometricsWatchOnlyCredentials
    get() = find { it.credential_type == CredentialType.BIOMETRICS_WATCHONLY_CREDENTIALS }

val List<LoginCredentials>.richWatchOnly
    get() = find { it.credential_type == CredentialType.RICH_WATCH_ONLY }

fun LoginCredentials.lightningMnemonic(
    greenKeystore: GreenKeystore,
    onError: ((exception: Exception) -> Unit) = {}
): String? {
    return try {
        if (credential_type != CredentialType.LIGHTNING_MNEMONIC) throw Exception("credential_type is not LIGHTNING_MNEMONIC")
        greenKeystore.decryptData(encrypted_data!!).decodeToString()
    } catch (e: Exception) {
        e.printStackTrace()
        onError.invoke(e)
        null
    }
}

fun LoginCredentials.boltzMnemonic(
    greenKeystore: GreenKeystore,
    onError: ((exception: Exception) -> Unit) = {}
): String? {
    return try {
        if (credential_type != CredentialType.BOLTZ_MNEMONIC) throw Exception("credential_type is not BOLTZ_MNEMONIC")
        greenKeystore.decryptData(encrypted_data!!).decodeToString()
    } catch (e: Exception) {
        e.printStackTrace()
        onError.invoke(e)
        null
    }
}

fun LoginCredentials.mnemonic(
    greenKeystore: GreenKeystore,
    onError: ((exception: Exception) -> Unit) = {}
): String? {
    return try {
        if (credential_type != CredentialType.KEYSTORE_MNEMONIC) throw Exception("credential_type is not KEYSTORE_MNEMONIC")
        greenKeystore.decryptData(encrypted_data!!).decodeToString()
    } catch (e: Exception) {
        e.printStackTrace()
        onError.invoke(e)
        null
    }
}

fun LoginCredentials.richWatchOnly(
    greenKeystore: GreenKeystore,
    onError: ((exception: Exception) -> Unit) = {}
): List<RichWatchOnly>? {
    return try {
        greenKeystore.decryptData(encrypted_data!!).decodeToString().toRichWatchOnly()
    } catch (e: Exception) {
        e.printStackTrace()
        onError.invoke(e)
        null
    }
}

// Time-based UUID similar to MongoDB ObjectId
// https://github.com/benasher44/uuid/issues/75
fun objectId(
    id: Long = Clock.System.now().toEpochMilliseconds()
): Uuid {
    val random = getSecureRandom().unsecureRandomLong()
    return Uuid.fromLongs(id, random)
}