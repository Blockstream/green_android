package com.blockstream.data.walletabi.provider

import com.blockstream.data.jade.JadeHWWallet
import com.blockstream.domain.domainModule
import com.blockstream.domain.walletabi.provider.WalletAbiProviderRunner
import com.blockstream.domain.walletabi.provider.WalletAbiProviderRunning
import io.mockk.mockk
import lwk.LwkException
import lwk.Network
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
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

    @Test
    fun device_factory_creates_real_device_signer() {
        val signer = WalletAbiJadePsetSignerFactory.Device.create(
            jadeWallet = mockk<JadeHWWallet>(),
            network = mockk<Network>(),
        )

        assertTrue(signer is WalletAbiLwkDeviceJadePsetSigner)
    }

    @Test
    fun domain_module_wires_provider_runner_to_real_device_signer_factory() {
        runCatching { stopKoin() }
        try {
            val koin = startKoin {
                modules(
                    module {
                        single {
                            mockk<WalletAbiEsploraHttpClient>()
                        }
                    },
                    domainModule,
                )
            }.koin

            val runner = koin.get<WalletAbiProviderRunning>()
            assertTrue(runner is WalletAbiProviderRunner)

            val factoryField = WalletAbiProviderRunner::class.java
                .getDeclaredField("jadePsetSignerFactory")
            factoryField.isAccessible = true
            val factory = factoryField.get(runner) as WalletAbiJadePsetSignerFactory
            val signer = factory.create(
                jadeWallet = mockk<JadeHWWallet>(),
                network = mockk<Network>(),
            )

            assertTrue(signer is WalletAbiLwkDeviceJadePsetSigner)
        } finally {
            stopKoin()
        }
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
