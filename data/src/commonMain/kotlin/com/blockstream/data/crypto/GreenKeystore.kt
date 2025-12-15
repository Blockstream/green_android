package com.blockstream.data.crypto

import com.blockstream.data.data.EncryptedData

typealias PlatformCipher = Any

class KeystoreInvalidatedException constructor(message: String) : Exception(message)

class BiometricsException constructor(message: String) : Exception(message)

interface GreenKeystore {
    fun encryptData(dataToEncrypt: ByteArray): EncryptedData
    fun encryptData(cipher: PlatformCipher, dataToEncrypt: ByteArray): EncryptedData
    fun decryptData(encryptedData: EncryptedData): ByteArray
    fun decryptData(cipher: PlatformCipher, encryptedData: EncryptedData): ByteArray
    fun canUseBiometrics(): Boolean
}