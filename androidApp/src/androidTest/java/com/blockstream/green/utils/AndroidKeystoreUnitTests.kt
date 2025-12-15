package com.blockstream.green.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.blockstream.data.utils.AndroidKeystore
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreUnitTests {
    private val ALIAS = "test-alias"
    private val MESSAGE = "A nice message to encrypt"

    private lateinit var androidKeystore: AndroidKeystore

    @Before
    fun setup() {
        androidKeystore = AndroidKeystore()
        androidKeystore.deleteFromKeyStore(ALIAS)
    }

    @Test
    fun test_keystore_encryption_decryption() {
        val encryptedData = androidKeystore.encryptData(MESSAGE.toByteArray())
        val decryptedBytes = androidKeystore.decryptData(encryptedData)
        Assert.assertEquals(String(decryptedBytes), MESSAGE)
    }

    @Test
    fun test_encryption_same_message_different_ciphertext() {
        val encryptedData1 = androidKeystore.encryptData(MESSAGE.toByteArray())
        val encryptedData2 = androidKeystore.encryptData(MESSAGE.toByteArray())

        Assert.assertNotEquals(encryptedData1, encryptedData2)

        val decryptedBytes1 = androidKeystore.decryptData(encryptedData1)
        val decryptedBytes2 = androidKeystore.decryptData(encryptedData2)

        Assert.assertEquals(String(decryptedBytes1), String(decryptedBytes2))
    }

    @Test(expected = Exception::class)
    fun on_duplicate_initialization_throw_exception() {
        androidKeystore.initializeKeyStoreKey(ALIAS, false)
        androidKeystore.initializeKeyStoreKey(ALIAS, false)
    }

    @Test
    fun test_key_alias_removal() {
        androidKeystore.initializeKeyStoreKey(ALIAS, false)
        androidKeystore.deleteFromKeyStore(ALIAS)
        androidKeystore.initializeKeyStoreKey(ALIAS, false)
    }
}