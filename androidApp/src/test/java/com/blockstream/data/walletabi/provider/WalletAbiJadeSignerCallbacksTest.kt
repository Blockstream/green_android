package com.blockstream.data.walletabi.provider

import com.blockstream.data.jade.JadeHWWallet
import io.mockk.mockk
import lwk.LwkException
import lwk.Pset
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class WalletAbiJadeSignerCallbacksTest {
    @Test
    fun signPst_delegates_to_injected_pset_signer() {
        val input = mockk<Pset>()
        val signed = mockk<Pset>()
        var captured: Pset? = null
        val psetSigner = object : WalletAbiJadePsetSigner {
            override fun sign(pset: Pset): Pset {
                captured = pset
                return signed
            }
        }

        val callbacks = WalletAbiJadeSignerCallbacks(
            jadeWallet = mockk<JadeHWWallet>(),
            psetSigner = psetSigner,
        )

        assertSame(signed, callbacks.signPst(input))
        assertSame(input, captured)
    }

    @Test
    fun signPst_keeps_real_transport_unsupported_by_default() {
        val callbacks = WalletAbiJadeSignerCallbacks(
            jadeWallet = mockk<JadeHWWallet>(),
        )

        val error = assertFailsWith<LwkException.Generic> {
            callbacks.signPst(mockk())
        }

        assertEquals(
            "Wallet ABI Jade PSET signing is not available for this Android Jade transport yet",
            error.msg,
        )
    }

    @Test
    fun signSchnorr_rejects_non_32_byte_message() {
        val callbacks = WalletAbiJadeSignerCallbacks(
            jadeWallet = mockk<JadeHWWallet>(),
            psetSigner = WalletAbiUnsupportedJadePsetSigner,
        )

        val error = assertFailsWith<LwkException.Generic> {
            callbacks.signSchnorr(ByteArray(31))
        }

        assertEquals("Wallet ABI Schnorr message must be 32 bytes", error.msg)
    }
}
