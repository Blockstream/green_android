package com.blockstream.data.walletabi.provider

import lwk.LwkException
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WalletAbiJadeTransportCallbacksTest {
    @Test
    fun read_returns_buffered_partial_chunks() {
        val callbacks = WalletAbiJadeTransportCallbacks(
            QueueJadeByteTransport(
                reads = ArrayDeque(
                    listOf(byteArrayOf(1, 2, 3, 4, 5)),
                ),
            ),
        )

        assertContentEquals(byteArrayOf(1, 2), callbacks.read(2u))
        assertContentEquals(byteArrayOf(3, 4), callbacks.read(2u))
        assertContentEquals(byteArrayOf(5), callbacks.read(2u))
    }

    @Test
    fun write_rejects_short_write() {
        val callbacks = WalletAbiJadeTransportCallbacks(
            object : WalletAbiJadeByteTransport {
                override fun write(bytes: ByteArray): Int = bytes.size - 1
                override fun read(): ByteArray = byteArrayOf(1)
            },
        )

        val error = assertFailsWith<LwkException.Generic> {
            callbacks.write(byteArrayOf(1, 2, 3))
        }

        assertTrue(error.msg.contains("wrote 2 of 3 bytes"))
    }

    @Test
    fun read_rejects_empty_transport_chunk() {
        val callbacks = WalletAbiJadeTransportCallbacks(
            QueueJadeByteTransport(
                reads = ArrayDeque(listOf(byteArrayOf())),
            ),
        )

        val error = assertFailsWith<LwkException.Generic> {
            callbacks.read(10u)
        }

        assertTrue(error.msg.contains("returned no bytes"))
    }

    @Test
    fun read_wraps_transport_failure() {
        val callbacks = WalletAbiJadeTransportCallbacks(
            object : WalletAbiJadeByteTransport {
                override fun write(bytes: ByteArray): Int = bytes.size
                override fun read(): ByteArray = throw IllegalStateException("boom")
            },
        )

        val error = assertFailsWith<LwkException.Generic> {
            callbacks.read(10u)
        }

        assertTrue(error.msg.contains("Wallet ABI Jade transport read failed: boom"))
    }

    private class QueueJadeByteTransport(
        private val reads: ArrayDeque<ByteArray>,
    ) : WalletAbiJadeByteTransport {
        override fun write(bytes: ByteArray): Int = bytes.size

        override fun read(): ByteArray {
            return reads.removeFirst()
        }
    }
}
