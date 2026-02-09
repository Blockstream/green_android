package com.blockstream.data.crypto

import com.blockstream.data.data.EncryptedData

// TODO encrypt with Key
class NoKeystore : GreenKeystore {
    override fun encryptData(dataToEncrypt: ByteArray): EncryptedData {
        return EncryptedData.fromByteArray(dataToEncrypt, byteArrayOf())
    }

    override fun encryptData(cipher: PlatformCipher, dataToEncrypt: ByteArray): EncryptedData {
        TODO("Not yet implemented")
    }

    override fun decryptData(encryptedData: EncryptedData): ByteArray {
        return encryptedData.getEncryptedData()
    }

    override fun decryptData(cipher: PlatformCipher, encryptedData: EncryptedData): ByteArray {
        TODO("Not yet implemented")
    }

    override fun canUseBiometrics(): Boolean = false
    override fun isBiometricEnrolled(): Boolean = false
}