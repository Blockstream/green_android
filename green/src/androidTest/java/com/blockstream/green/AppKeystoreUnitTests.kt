package com.blockstream.green

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Exception


@RunWith(AndroidJUnit4::class)
class AppKeystoreUnitTests {
    private val ALIAS = "test-alias"
    private val MESSAGE = "A nice message to encrypt"

    private lateinit var appKeystore: AppKeystore

    @Before
    fun setup() {
        appKeystore = AppKeystore()
        appKeystore.deleteFromKeyStore(ALIAS)
    }

    @Test
    fun test_keystore_encryption_decryption() {
        val encryptedData = appKeystore.encryptData(MESSAGE.toByteArray())
        val decryptedBytes = appKeystore.decryptData(encryptedData)
        Assert.assertEquals(String(decryptedBytes), MESSAGE)
    }

    @Test
    fun test_encryption_same_message_different_ciphertext() {
        val encryptedData1 = appKeystore.encryptData(MESSAGE.toByteArray())
        val encryptedData2 = appKeystore.encryptData(MESSAGE.toByteArray())

        Assert.assertNotEquals(encryptedData1, encryptedData2)

        val decryptedBytes1 = appKeystore.decryptData(encryptedData1)
        val decryptedBytes2 = appKeystore.decryptData(encryptedData2)

        Assert.assertEquals(String(decryptedBytes1), String(decryptedBytes2))
    }

    @Test(expected = Exception::class)
    fun on_duplicate_initialization_throw_exception() {
        appKeystore.initializeKeyStoreKey(ALIAS, false)
        appKeystore.initializeKeyStoreKey(ALIAS, false)
    }

    @Test
    fun test_key_alias_removal() {
        appKeystore.initializeKeyStoreKey(ALIAS, false)
        appKeystore.deleteFromKeyStore(ALIAS)
        appKeystore.initializeKeyStoreKey(ALIAS, false)
    }
}