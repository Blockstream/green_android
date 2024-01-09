package com.blockstream.common.utils

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import androidx.annotation.VisibleForTesting
import com.blockstream.common.crypto.GreenKeystore
import com.blockstream.common.crypto.PlatformCipher
import com.blockstream.common.data.EncryptedData
import java.security.*
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec

class AndroidKeystore(val context: Context) : GreenKeystore {
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }

    fun isBiometricsAuthenticationRequired(): Boolean {
        try {
            getEncryptionCipher(BIOMETRICS_KEYSTORE_ALIAS)
        } catch (e: UserNotAuthenticatedException) {
            logger.i { "User not Authenticated, probably device was unlocked with Face biometrics" }
            return true
        } catch (e: Exception) {
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

        // Even if we weren't able to get the Cipher, we expect the key to be valid
        return true
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

            builder.setInvalidatedByBiometricEnrollment(true)
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
    fun getBiometricsEncryptionCipher(recreateKeyIfNeeded: Boolean): Cipher {
        if ((!keyStoreKeyExists(BIOMETRICS_KEYSTORE_ALIAS) || !isKeyStoreValid(
                BIOMETRICS_KEYSTORE_ALIAS
            )) && recreateKeyIfNeeded
        ) {
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
    fun getBiometricsDecryptionCipher(
        encryptedData: EncryptedData,
        keystoreAlias: String? = null
    ): Cipher {
        return getDecryptionCipher(keystoreAlias ?: BIOMETRICS_KEYSTORE_ALIAS, encryptedData)
    }

    @Throws(Exception::class)
    override fun encryptData(dataToEncrypt: ByteArray): EncryptedData {
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

    override fun encryptData(cipher: PlatformCipher, dataToEncrypt: ByteArray): EncryptedData {
        return encryptData(cipher as Cipher, dataToEncrypt)
    }

    @Throws(Exception::class)
    override fun decryptData(encryptedData: EncryptedData): ByteArray {
        if (!keyStoreKeyExists(BASIC_KEYSTORE_ALIAS)) {
            throw KeyStoreException("KeyStore Keys are not created. You have to init them first.")
        }

        val cipher = getDecryptionCipher(BASIC_KEYSTORE_ALIAS, encryptedData)

        return decryptData(cipher, encryptedData)
    }

    @Throws(Exception::class)
    override fun decryptData(cipher: PlatformCipher, encryptedData: EncryptedData): ByteArray {
        return (cipher as Cipher).doFinal(encryptedData.getEncryptedData())
    }

    override fun canUseBiometrics(): Boolean {
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

    companion object : Loggable() {
        private const val TRANSFORMATION =
            "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"

        const val BASIC_KEYSTORE_ALIAS = "v1-basic"
        const val BIOMETRICS_KEYSTORE_ALIAS = "v1-biometrics"
    }
}