package com.blockstream.green.utils

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.util.Base64
import androidx.annotation.VisibleForTesting
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.*
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec

class AppKeystore {
    private var keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    fun isBiometricsAuthenticationRequired(): Boolean {
        try{
            getEncryptionCipher(BIOMETRICS_KEYSTORE_ALIAS)
        }catch (e: UserNotAuthenticatedException){
            e.printStackTrace()
            return true
        }catch (e: Exception){
            e.printStackTrace()
        }

        return false
    }

    private fun keyStoreKeyExists(keystoreAlias: String): Boolean {
        try {
            return keyStore.containsAlias(keystoreAlias)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun deleteFromKeyStore(keystoreAlias: String) {
        try {
            // remove keystore alias
            keyStore.deleteEntry(keystoreAlias)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isKeyStoreValid(keystoreAlias: String): Boolean {
        try {
            getEncryptionCipher(keystoreAlias)
            return true
        } catch (e: KeyPermanentlyInvalidatedException) {
            e.printStackTrace()
            return false
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @Throws(Exception::class)
    fun initializeKeyStoreKey(keystoreAlias: String, isBiometric: Boolean) {
        if (keyStoreKeyExists(keystoreAlias)) {
            throw KeyStoreException("KeyStore is already created for $keystoreAlias")
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val builder = KeyGenParameterSpec.Builder(
            keystoreAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)


        if (isBiometric) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder.setUnlockedDeviceRequired(true)
            }

            builder.setUserAuthenticationRequired(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            } else {
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(-1)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(true)
            }
        }

        keyGenerator.init(builder.build())

        // Add it to KeyStore
        keyGenerator.generateKey()
    }

    @Throws(Exception::class)
    private fun getEncryptionCipher(keystoreAlias: String): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val key = keyStore.getKey(keystoreAlias, null) as SecretKey
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher
    }

    @Throws(Exception::class)
    private fun getDecryptionCipher(keystoreAlias: String, encryptedData: EncryptedData): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val key = keyStore.getKey(keystoreAlias, null)
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(encryptedData.getIv()))
        return cipher
    }

    /**
     * The Key for Biometric use can be invalidated if a new fingerprint is enrolled as the key is
     * instantiated with {@code setInvalidatedByBiometricEnrollment} option enabled.
     * Recreate the keystore key if the key is no longer valid.
     *
     * The returned {@code Cipher} object must be unlocked by {@code BiometricPrompt} before use.
     */
    @Throws(Exception::class)
    fun getBiometricsEncryptionCipher(): Cipher {
        if (!keyStoreKeyExists(BIOMETRICS_KEYSTORE_ALIAS) || !isKeyStoreValid(
                BIOMETRICS_KEYSTORE_ALIAS
            )) {
            deleteFromKeyStore(BIOMETRICS_KEYSTORE_ALIAS)
            initializeKeyStoreKey(BIOMETRICS_KEYSTORE_ALIAS, true)
        }

        return getEncryptionCipher(BIOMETRICS_KEYSTORE_ALIAS)
    }

    /**
     * In most cases @{code keystoreAlias} should be set to null to use the default key @{code BIOMETRICS_KEYSTORE_ALIAS}.
     * If you want to decrypt data from a different KeyStore (eg. migrated data) and the same decryption settings,
     * you can provide the alias to be used.
     */
    @Throws(Exception::class)
    fun getBiometricsDecryptionCipher(encryptedData: EncryptedData, keystoreAlias: String? = null): Cipher {
        return getDecryptionCipher(keystoreAlias ?: BIOMETRICS_KEYSTORE_ALIAS, encryptedData)
    }

    @Throws(Exception::class)
    fun encryptData(dataToEncrypt: ByteArray): EncryptedData {
        if (!keyStoreKeyExists(BASIC_KEYSTORE_ALIAS)) {
            initializeKeyStoreKey(BASIC_KEYSTORE_ALIAS, false)
        }

        val cipher = getEncryptionCipher(BASIC_KEYSTORE_ALIAS)

        return encryptData(cipher, dataToEncrypt)
    }

    @Throws(Exception::class)
    fun encryptData(cipher: Cipher, dataToEncrypt: ByteArray): EncryptedData {
        return EncryptedData.fromByteArray(cipher.doFinal(dataToEncrypt), cipher.iv)
    }

    @Throws(Exception::class)
    fun decryptData(encryptedData: EncryptedData): ByteArray {
        if (!keyStoreKeyExists(BASIC_KEYSTORE_ALIAS)) {
            throw KeyStoreException("KeyStore Keys are not created. You have to init them first.")
        }

        val cipher = getDecryptionCipher(BASIC_KEYSTORE_ALIAS, encryptedData)

        return decryptData(cipher, encryptedData)
    }

    @Throws(Exception::class)
    fun decryptData(cipher: Cipher, encryptedData: EncryptedData): ByteArray {
        return cipher.doFinal(encryptedData.getEncryptedData())
    }

    fun canUseBiometrics(context: Context): Boolean {
        val kManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        try {
            if (kManager.isDeviceSecure) {
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    companion object {
        private const val TRANSFORMATION =
            "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"

        const val BASIC_KEYSTORE_ALIAS = "v1-basic"
        const val BIOMETRICS_KEYSTORE_ALIAS = "v1-biometrics"
    }
}

@Serializable
data class EncryptedData(private val encryptedData: String, private val iv: String) {
    fun getEncryptedData(): ByteArray = Base64.decode(encryptedData, Base64.NO_WRAP)
    fun getIv(): ByteArray = Base64.decode(iv, Base64.NO_WRAP)

    override fun toString(): String = Json.encodeToString(this)

    companion object {
        fun fromByteArray(encryptedData: ByteArray, iv: ByteArray) = EncryptedData(
            Base64.encodeToString(encryptedData, Base64.NO_WRAP),
            Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }
}