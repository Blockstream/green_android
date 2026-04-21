package com.blockstream.data.walletabi.provider

import com.blockstream.jade.JadeRawTransport
import kotlinx.coroutines.runBlocking
import lwk.JadeTransportCallbacks
import lwk.LwkException

interface WalletAbiJadeByteTransport {
    fun write(bytes: ByteArray): Int
    fun read(): ByteArray
}

private class WalletAbiJadeRawByteTransport(
    private val rawTransport: JadeRawTransport,
) : WalletAbiJadeByteTransport {
    override fun write(bytes: ByteArray): Int {
        return runBlocking {
            rawTransport.write(bytes)
        }
    }

    override fun read(): ByteArray {
        return runBlocking {
            rawTransport.read()
        }
    }
}

class WalletAbiJadeTransportCallbacks(
    private val transport: WalletAbiJadeByteTransport,
) : JadeTransportCallbacks {
    constructor(rawTransport: JadeRawTransport) : this(WalletAbiJadeRawByteTransport(rawTransport))

    private var buffered = byteArrayOf()

    override fun write(bytes: ByteArray) {
        wrapTransportError("write") {
            val written = transport.write(bytes)
            if (written != bytes.size) {
                throw LwkException.Generic(
                    "Wallet ABI Jade transport write failed: wrote $written of ${bytes.size} bytes",
                )
            }
        }
    }

    override fun read(maxLen: UInt): ByteArray {
        return wrapTransportError("read") {
            if (maxLen == 0u) {
                throw LwkException.Generic("Wallet ABI Jade transport read failed: maxLen is 0")
            }

            val max = maxLen.coerceAtMost(Int.MAX_VALUE.toUInt()).toInt()
            if (buffered.isNotEmpty()) {
                return@wrapTransportError takeBuffered(max)
            }

            val bytes = transport.read()
            if (bytes.isEmpty()) {
                throw LwkException.Generic("Wallet ABI Jade transport read failed: returned no bytes")
            }
            if (bytes.size <= max) {
                bytes
            } else {
                buffered = bytes.copyOfRange(max, bytes.size)
                bytes.copyOfRange(0, max)
            }
        }
    }

    private fun takeBuffered(max: Int): ByteArray {
        if (buffered.size <= max) {
            return buffered.also {
                buffered = byteArrayOf()
            }
        }

        val next = buffered.copyOfRange(0, max)
        buffered = buffered.copyOfRange(max, buffered.size)
        return next
    }

    private inline fun <T> wrapTransportError(operation: String, block: () -> T): T {
        try {
            return block()
        } catch (e: LwkException) {
            throw e
        } catch (e: Exception) {
            throw LwkException.Generic(
                "Wallet ABI Jade transport $operation failed: ${e.message ?: e::class.simpleName}",
            )
        }
    }
}
