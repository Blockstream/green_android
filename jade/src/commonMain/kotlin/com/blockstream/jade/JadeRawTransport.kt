package com.blockstream.jade

import com.blockstream.jade.connection.JadeConnection

class JadeRawTransport internal constructor(
    private val connection: JadeConnection,
) {
    suspend fun write(bytes: ByteArray): Int {
        return connection.write(bytes)
    }

    suspend fun read(): ByteArray {
        return connection.read(timeout = -1) ?: byteArrayOf()
    }
}
