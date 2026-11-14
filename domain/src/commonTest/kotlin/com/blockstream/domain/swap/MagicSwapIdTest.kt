/**
 * Tests for CreateNormalSubmarineSwapUseCase.magicSwapId: a magic-routing swap must carry
 * the id of the already stored unpaid magic row when one exists, so that the funding
 * transaction's tx_hash lands on that row and marks the invoice as paid. A fresh id is
 * only valid when a new row is about to be stored under it.
 */
package com.blockstream.domain.swap

import com.blockstream.data.data.SwapType
import com.blockstream.data.database.wallet.BoltzSwaps
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MagicSwapIdTest {

    private fun storedSwap(isMagic: Boolean) = BoltzSwaps(
        id = "stored-row-id",
        wallet_id = "wallet",
        xpub_hash_id = "xpub",
        invoice = "lnbc1invoice",
        tx_hash = null,
        is_pending = true,
        swap_type = SwapType.NormalSubmarine,
        is_auto_swap = false,
        is_magic = isMagic,
        data_ = ""
    )

    @Test
    fun reusesStoredMagicSwapId() {
        assertEquals(
            "stored-row-id",
            CreateNormalSubmarineSwapUseCase.magicSwapId(storedSwap(isMagic = true))
        )
    }

    @Test
    fun generatesIdWhenNoSwapIsStored() {
        assertTrue(CreateNormalSubmarineSwapUseCase.magicSwapId(null).isNotBlank())
    }

    @Test
    fun generatesFreshIdForNonMagicSwap() {
        assertNotEquals(
            "stored-row-id",
            CreateNormalSubmarineSwapUseCase.magicSwapId(storedSwap(isMagic = false))
        )
    }
}
