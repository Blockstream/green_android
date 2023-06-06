package com.blockstream.common.utils

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault

private fun byteToInt(bytes: ByteArray): Int {
    var result = 0
    var shift = 0
    for (byte in bytes) {
        result = result or (byte.toInt() shl shift)
        shift += 8
    }
    return result
}

class IOSSecureRandom : SecureRandom {
    override fun randomBytes(len: Int): ByteArray {
        return ByteArray(len).also {

            if (it.isEmpty()) return byteArrayOf()

            it.usePinned { pin ->
                val ptr = pin.addressOf(0)
                val status = SecRandomCopyBytes(kSecRandomDefault, it.size.convert(), ptr)
                if (status != errSecSuccess) {
                    error("Error filling random bytes. errorCode=$status")
                }
            }
        }
    }

    override fun unsecureRandomInt(): Int {
        return byteToInt(randomBytes(Int.SIZE_BYTES))
    }

    override fun unsecureRandomInt(until: Int): Int {
        return (0..until).random()
    }
}


actual fun getSecureRandom(): SecureRandom {
    return IOSSecureRandom()
}