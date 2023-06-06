package com.blockstream.common.utils


interface SecureRandom {
    fun randomBytes(len: Int): ByteArray

    fun unsecureRandomInt(): Int

    fun unsecureRandomInt(until: Int): Int
}

fun randomChars(len: Int): String {
    val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    val random = getSecureRandom()
    return (1..len)
        .map { random.unsecureRandomInt(charPool.size) }
        .map(charPool::get)
        .joinToString("")
}

expect fun getSecureRandom(): SecureRandom
