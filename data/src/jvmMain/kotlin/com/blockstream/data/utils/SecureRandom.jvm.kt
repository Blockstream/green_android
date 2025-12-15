package com.blockstream.data.utils

import kotlin.random.asKotlinRandom

private val secureRandom = java.security.SecureRandom()

class AndroidSecureRandom : SecureRandom {
    override fun randomBytes(len: Int): ByteArray {
        return ByteArray(len).also {
            secureRandom.nextBytes(it)
        }
    }

    override fun unsecureRandomLong(): Long {
        return secureRandom.nextLong()
    }

    override fun unsecureRandomInt(): Int {
        return secureRandom.nextInt()
    }

    override fun unsecureRandomInt(until: Int): Int {
        return secureRandom.nextInt(until)
    }

    override fun unsecureRandomInt(from: Int, until: Int): Int {
        return secureRandom.asKotlinRandom().nextInt(from, until)
    }
}

actual fun getSecureRandom(): SecureRandom {
    return AndroidSecureRandom()
}