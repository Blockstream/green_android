package com.blockstream.common.utils

private val secureRandom = java.security.SecureRandom()

class AndroidSecureRandom : SecureRandom {
    override fun randomBytes(len: Int): ByteArray {
        return ByteArray(len).also {
            secureRandom.nextBytes(it)
        }
    }

    override fun unsecureRandomInt(): Int {
        return secureRandom.nextInt()
    }

    override fun unsecureRandomInt(until: Int): Int {
        return secureRandom.nextInt(until)
    }
}

actual fun getSecureRandom(): SecureRandom {
    return AndroidSecureRandom()
}