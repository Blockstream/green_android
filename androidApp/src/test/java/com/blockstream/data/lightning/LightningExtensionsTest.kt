// Tests for pure extension functions in Extensions.kt (lightning subsystem).
// All functions here operate on plain data types — no mocking required.
package com.blockstream.data.lightning

import com.blockstream.data.data.FeePriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LightningExtensionsTest {

    // region milliSatoshi / satoshi

    @Test
    fun `milliSatoshi multiplies by 1000`() {
        assertEquals(0UL, 0L.milliSatoshi())
        assertEquals(1000UL, 1L.milliSatoshi())
        assertEquals(21_000_000_000UL, 21_000_000L.milliSatoshi())
    }

    @Test
    fun `satoshi divides by 1000 truncating remainder`() {
        assertEquals(0L, 0UL.satoshi())
        assertEquals(0L, 999UL.satoshi())   // sub-satoshi truncated
        assertEquals(1L, 1000UL.satoshi())
        assertEquals(1L, 1999UL.satoshi())  // remainder truncated
        assertEquals(21_000_000L, 21_000_000_000UL.satoshi())
    }

    @Test
    fun `milliSatoshi and satoshi are inverse for whole satoshi values`() {
        listOf(0L, 1L, 100L, 21_000_000L).forEach { sats ->
            assertEquals(sats, sats.milliSatoshi().satoshi())
        }
    }

    // endregion

    // region LightningInvoice helpers

    private fun invoice(
        amountSatoshi: Long? = null,
        timestamp: Long = 0,
        expiry: Long = 3600,
    ) = LightningInvoice(
        bolt11 = "lnbc1...",
        amountSatoshi = amountSatoshi,
        timestamp = timestamp,
        expiry = expiry,
        paymentHash = "deadbeef",
        description = null,
    )

    @Test
    fun `isAmountLocked is true when invoice carries an amount`() {
        assertTrue(invoice(amountSatoshi = 1000L).isAmountLocked())
    }

    @Test
    fun `isAmountLocked is false for zero-amount invoice`() {
        assertFalse(invoice(amountSatoshi = null).isAmountLocked())
    }

    @Test
    fun `sendableSatoshi returns invoice amount when locked, ignoring user input`() {
        assertEquals(500L, invoice(amountSatoshi = 500L).sendableSatoshi(userSatoshi = 999L))
    }

    @Test
    fun `sendableSatoshi returns user amount when invoice has no fixed amount`() {
        assertEquals(300L, invoice(amountSatoshi = null).sendableSatoshi(userSatoshi = 300L))
    }

    @Test
    fun `sendableSatoshi returns null when invoice has no amount and user provides none`() {
        assertNull(invoice(amountSatoshi = null).sendableSatoshi(userSatoshi = null))
    }

    @Test
    fun `receiveAmountSatoshi subtracts opening fee from invoice amount`() {
        assertEquals(900L, invoice(amountSatoshi = 1000L).receiveAmountSatoshi(openingFeeSatoshi = 100L))
    }

    @Test
    fun `receiveAmountSatoshi treats missing invoice amount as zero`() {
        assertEquals(-100L, invoice(amountSatoshi = null).receiveAmountSatoshi(openingFeeSatoshi = 100L))
    }

    @Test
    fun `isExpired returns true for an invoice that expired in the past`() {
        // timestamp in 2020, expiry 1 second → long expired
        assertTrue(invoice(timestamp = 1_580_000_000L, expiry = 1L).isExpired())
    }

    @Test
    fun `isExpired returns false for an invoice that expires far in the future`() {
        // timestamp far in future → not yet expired
        assertFalse(invoice(timestamp = 9_999_999_999L, expiry = 3600L).isExpired())
    }

    // endregion

    // region LightningNodeState helpers

    private fun nodeState(
        id: String = "nodeid",
        channelsMsat: ULong = 0u,
        onchainMsat: ULong = 0u,
        maxPayableMsat: ULong = 0u,
        maxReceivableMsat: ULong = 0u,
        maxSinglePaymentMsat: ULong = 0u,
        totalInboundMsat: ULong = 0u,
    ) = LightningNodeState(
        id = id,
        blockHeight = 0u,
        channelsBalanceMsat = channelsMsat,
        onchainBalanceMsat = onchainMsat,
        pendingOnchainBalanceMsat = 0u,
        maxPayableMsat = maxPayableMsat,
        maxReceivableMsat = maxReceivableMsat,
        maxSinglePaymentAmountMsat = maxSinglePaymentMsat,
        maxChanReserveMsats = 0u,
        maxReceivableSinglePaymentAmountMsat = 0u,
        totalInboundLiquidityMsats = totalInboundMsat,
        connectedPeers = emptyList(),
    )

    @Test
    fun `isLoading is true when node id is blank`() {
        assertTrue(nodeState(id = "").isLoading())
    }

    @Test
    fun `isLoading is false when node id is set`() {
        assertFalse(nodeState(id = "03abc123").isLoading())
    }

    @Test
    fun `channelsBalanceSatoshi converts channel msat balance to sat`() {
        assertEquals(5L, nodeState(channelsMsat = 5000u).channelsBalanceSatoshi())
    }

    @Test
    fun `onchainBalanceSatoshi converts onchain msat balance to sat`() {
        assertEquals(2L, nodeState(onchainMsat = 2000u).onchainBalanceSatoshi())
    }

    @Test
    fun `maxPayableSatoshi converts max payable msat to sat`() {
        assertEquals(10L, nodeState(maxPayableMsat = 10_000u).maxPayableSatoshi())
    }

    @Test
    fun `maxReceivableSatoshi converts max receivable msat to sat`() {
        assertEquals(8L, nodeState(maxReceivableMsat = 8_000u).maxReceivableSatoshi())
    }

    @Test
    fun `maxSinglePaymentAmountSatoshi converts msat to sat`() {
        assertEquals(3L, nodeState(maxSinglePaymentMsat = 3_000u).maxSinglePaymentAmountSatoshi())
    }

    @Test
    fun `totalInboundLiquiditySatoshi converts msat to sat`() {
        assertEquals(100L, nodeState(totalInboundMsat = 100_000u).totalInboundLiquiditySatoshi())
    }

    // endregion

    // region LightningPayment.amountSatoshi

    private fun payment(amountMsat: ULong, type: LightningPaymentType) = LightningPayment(
        id = "pay_id",
        paymentType = type,
        paymentTime = 0L,
        amountMsat = amountMsat,
        feeMsat = 0u,
        status = LightningPaymentStatus.COMPLETE,
        description = null,
        details = LightningPaymentDetails.Ln(
            destinationPubkey = "",
            paymentHash = "",
            paymentPreimage = "",
            bolt11 = "",
            successAction = null,
        ),
    )

    @Test
    fun `amountSatoshi is positive for received payments`() {
        assertEquals(5L, payment(5_000u, LightningPaymentType.RECEIVED).amountSatoshi())
    }

    @Test
    fun `amountSatoshi is negative for sent payments`() {
        assertEquals(-5L, payment(5_000u, LightningPaymentType.SENT).amountSatoshi())
    }

    @Test
    fun `amountSatoshi is negative for closed channel payments`() {
        assertEquals(-3L, payment(3_000u, LightningPaymentType.CLOSED_CHANNEL).amountSatoshi())
    }

    // endregion

    // region LNURL metadata helpers

    @Test
    fun `lnUrlPayDescription extracts text-plain from metadata`() {
        val metadata = listOf(
            listOf("text/plain", "Send me sats"),
            listOf("image/png;base64", "abc123"),
        )
        assertEquals("Send me sats", metadata.lnUrlPayDescription())
    }

    @Test
    fun `lnUrlPayDescription returns null when no text-plain entry exists`() {
        val metadata = listOf(listOf("image/png;base64", "abc123"))
        assertNull(metadata.lnUrlPayDescription())
    }

    @Test
    fun `lnUrlPayDescription returns null for null list`() {
        assertNull(null.lnUrlPayDescription())
    }

    // endregion

    // region LnUrlWithdrawData helpers

    private fun withdrawData(
        callback: String = "https://service.example.com/withdraw",
        minMsat: ULong = 1_000u,
        maxMsat: ULong = 10_000u,
    ) = LnUrlWithdrawData(
        callback = callback,
        k1 = "k1value",
        defaultDescription = "Withdraw",
        minWithdrawable = minMsat,
        maxWithdrawable = maxMsat,
    )

    @Test
    fun `maxWithdrawableSatoshi converts msat to sat`() {
        assertEquals(10L, withdrawData(maxMsat = 10_000u).maxWithdrawableSatoshi())
    }

    @Test
    fun `minWithdrawableSatoshi converts msat to sat`() {
        assertEquals(1L, withdrawData(minMsat = 1_000u).minWithdrawableSatoshi())
    }

    @Test
    fun `domain extracts hostname from callback URL`() {
        assertEquals("service.example.com", withdrawData(callback = "https://service.example.com/withdraw").domain())
    }

    @Test
    fun `domain strips port from callback URL`() {
        assertEquals("service.example.com", withdrawData(callback = "https://service.example.com:8080/withdraw").domain())
    }

    // endregion

    // region LightningFees.fee

    private fun fees() = LightningFees(
        fastestFee = 20u,
        halfHourFee = 15u,
        hourFee = 10u,
        economyFee = 5u,
        minimumFee = 2u,
    )

    @Test
    fun `fee returns fastestFee for High priority`() {
        assertEquals(20L, fees().fee(FeePriority.High()))
    }

    @Test
    fun `fee returns hourFee for Medium priority`() {
        assertEquals(10L, fees().fee(FeePriority.Medium()))
    }

    @Test
    fun `fee returns economyFee for Low priority`() {
        assertEquals(5L, fees().fee(FeePriority.Low()))
    }

    @Test
    fun `fee returns custom rate when above minimum`() {
        assertEquals(8L, fees().fee(FeePriority.Custom(customFeeRate = 8.0)))
    }

    @Test
    fun `fee clamps custom rate up to minimumFee when below minimum`() {
        assertEquals(2L, fees().fee(FeePriority.Custom(customFeeRate = 1.0)))
    }

    // endregion
}
