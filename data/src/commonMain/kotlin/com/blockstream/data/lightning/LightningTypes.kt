/**
 * SDK-agnostic data types for the lightning subsystem. LightningBridge maps to/from these at
 * its boundaries, isolating the rest of the app from the underlying breez_sdk implementation.
 *
 * LnUrlPayData carries an internal `raw` field because LnUrlPayRequestData has fields beyond
 * what we expose, making full reconstruction unsafe without knowing the complete SDK type.
 * LnUrlAuthData and LnUrlWithdrawData are plain @Serializable data classes since all their
 * fields are known.
 */
@file:OptIn(ExperimentalUnsignedTypes::class)

package com.blockstream.data.lightning

import kotlinx.serialization.Serializable

enum class LightningHealthStatus {
    OPERATIONAL, MAINTENANCE, SERVICE_DISRUPTION
}

enum class LightningPaymentType {
    RECEIVED, SENT, CLOSED_CHANNEL
}

enum class LightningPaymentStatus {
    PENDING, COMPLETE, FAILED
}

enum class LightningChannelState {
    PENDING_OPEN,
    OPENED,
    PENDING_CLOSE,
    CLOSED
}

// COMPLETED_SEEN = transaction seen on chain; COMPLETED_CONFIRMED = sufficiently confirmed.
enum class LightningReverseSwapStatus {
    INITIAL,
    IN_PROGRESS,
    CANCELLED,
    COMPLETED_SEEN,
    COMPLETED_CONFIRMED;
}

data class LightningNodeState(
    val id: String,
    val blockHeight: UInt,
    val channelsBalanceMsat: ULong,
    val onchainBalanceMsat: ULong,
    val pendingOnchainBalanceMsat: ULong,
    val maxPayableMsat: ULong,
    val maxReceivableMsat: ULong,
    val maxSinglePaymentAmountMsat: ULong,
    val maxChanReserveMsats: ULong,
    val maxReceivableSinglePaymentAmountMsat: ULong,
    val totalInboundLiquidityMsats: ULong,
    val connectedPeers: List<String>
) {
    companion object {
        val Default = LightningNodeState(
            id = "",
            blockHeight = 0u,
            channelsBalanceMsat = 0u,
            onchainBalanceMsat = 0u,
            pendingOnchainBalanceMsat = 0u,
            maxPayableMsat = 0u,
            maxReceivableMsat = 0u,
            maxSinglePaymentAmountMsat = 0u,
            maxChanReserveMsats = 0u,
            connectedPeers = listOf(),
            maxReceivableSinglePaymentAmountMsat = 0u,
            totalInboundLiquidityMsats = 0u
        )
    }
}

sealed class LightningEvent {
    object Synced : LightningEvent()
    data class NewBlock(val block: UInt) : LightningEvent()
    data class InvoicePaid(val paymentHash: String, val paymentAmountSatoshi: Long?) : LightningEvent()
}

// Submarine swap: user deposits onchain BTC and receives lightning satoshis.
data class LightningSwapInfo(
    val bitcoinAddress: String,
    val bolt11: String?,
    val refundTxIds: List<String>,
    val confirmedTxIds: List<String>,
    val unconfirmedTxIds: List<String>,
    val paymentHash: ByteArray,
    val confirmedSats: ULong,
    val unconfirmedSats: ULong,
    val createdAt: Long,
    val minAllowedDeposit: Long,
    val maxAllowedDeposit: Long,
    val channelOpeningFees: LightningOpeningFeeParams?,
)

// Reverse submarine swap: user sends lightning satoshis and receives onchain BTC.
data class LightningReverseSwapInfo(
    val id: String,
    val claimTxid: String?,
    val lockupTxid: String?,
    val onchainAmountSat: ULong,
    val status: LightningReverseSwapStatus,
)

// Parsed result of a user-provided payment string (BOLT11, LNURL-pay, LNURL-withdraw, LNURL-auth).
sealed class LightningInputType {
    data class Bolt11(val invoice: LightningInvoice) : LightningInputType()
    data class LnUrlPay(val data: LnUrlPayData) : LightningInputType()
    data class LnUrlWithdraw(val data: LnUrlWithdrawData) : LightningInputType()
    data class LnUrlAuth(val data: LnUrlAuthData) : LightningInputType()
}

data class LightningInvoice(
    val bolt11: String,
    val amountSatoshi: Long?,
    val timestamp: Long,
    val expiry: Long,
    val paymentHash: String,
    val description: String?,
)

// LNURL-pay request data. Carries `raw` internally because the SDK type has additional fields
// (commentAllowed, allowsNostr, etc.) that are not exposed but are needed for SDK calls.
class LnUrlPayData internal constructor(
    val domain: String,
    val minSendable: ULong,
    val maxSendable: ULong,
    val metadataStr: String,
    internal val raw: breez_sdk.LnUrlPayRequestData,
)

@Serializable
data class LnUrlWithdrawData(
    val callback: String,
    val k1: String,
    val defaultDescription: String,
    val minWithdrawable: ULong,
    val maxWithdrawable: ULong,
)

@Serializable
data class LnUrlAuthData(
    val k1: String,
    val domain: String,
    val url: String,
    val action: String? = null,
)

// What to show the user after a successful LNURL-pay (message, URL, or AES-decrypted content).
sealed class LightningSuccessAction {
    data class Message(val message: String) : LightningSuccessAction()
    data class Url(val description: String, val url: String) : LightningSuccessAction()
    data class Aes(val description: String, val plaintext: String) : LightningSuccessAction()
}

sealed class LightningPaymentDetails {
    data class Ln(
        val destinationPubkey: String,
        val paymentHash: String,
        val paymentPreimage: String,
        val bolt11: String,
        val successAction: LightningSuccessAction?,
    ) : LightningPaymentDetails()

    data class ClosedChannel(
        val fundingTxid: String,
        val closingTxid: String?,
        val state: LightningChannelState,
    ) : LightningPaymentDetails()
}

data class LightningPayment(
    val id: String,
    val paymentType: LightningPaymentType,
    val paymentTime: Long,
    val amountMsat: ULong,
    val feeMsat: ULong,
    val status: LightningPaymentStatus,
    val description: String?,
    val details: LightningPaymentDetails,
)

// Recommended on-chain fee rates in sat/vbyte, used to populate fee priority selectors.
data class LightningFees(
    val fastestFee: ULong,
    val halfHourFee: ULong,
    val hourFee: ULong,
    val economyFee: ULong,
    val minimumFee: ULong,
)

// Carries the opening fee amount and the opaque params needed to create an invoice at that fee tier.
class LightningOpeningFeeParams internal constructor(
    val minMsat: ULong,
    internal val raw: breez_sdk.OpeningFeeParams,
)

// Fee charged when a new channel must be opened to receive a payment.
data class ChannelOpenFee(
    val feeMsat: ULong?,
    val openingFeeParams: LightningOpeningFeeParams?,
)

data class OnchainLimits(val maxPayableSat: ULong)

// totalFees is null when queried without a specific send amount.
data class ReverseSwapLimits(val min: ULong, val max: ULong, val totalFees: ULong?)

data class LightningReceivePayment(
    val invoice: LightningInvoice,
    val openingFeeSatoshi: Long,
)

data class PrepareRefundResult(val refundTxFeeSat: ULong)

data class PrepareRedeemOnchainResult(val txFeeSat: ULong)

data class RefundResult(val refundTxId: String)

data class RedeemOnchainResult(val txid: List<UByte>)

data class SendPaymentResult(val paymentId: String)

sealed class LnUrlPayOutcome {
    data class Success(val message: String?, val url: String?) : LnUrlPayOutcome()
    data class Error(val reason: String) : LnUrlPayOutcome()
    data class PayError(val reason: String, val paymentHash: String) : LnUrlPayOutcome()
}

sealed class LnUrlAuthOutcome {
    object Ok : LnUrlAuthOutcome()
    data class Error(val reason: String) : LnUrlAuthOutcome()
}

sealed class LnUrlWithdrawOutcome {
    object Ok : LnUrlWithdrawOutcome()
    data class Error(val reason: String) : LnUrlWithdrawOutcome()
}
