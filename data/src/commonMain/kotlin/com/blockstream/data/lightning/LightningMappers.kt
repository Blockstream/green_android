/**
 * Internal conversion functions from breez_sdk types to LightningTypes DTOs.
 * Only used within the data module's lightning boundary.
 */
@file:OptIn(ExperimentalUnsignedTypes::class)

package com.blockstream.data.lightning

import breez_sdk.AesSuccessActionDataResult
import breez_sdk.BreezEvent
import breez_sdk.ChannelState
import breez_sdk.HealthCheckStatus
import breez_sdk.InputType
import breez_sdk.LnUrlCallbackStatus
import breez_sdk.LnUrlPayResult
import breez_sdk.LnUrlWithdrawResult
import breez_sdk.NodeState
import breez_sdk.OnchainPaymentLimitsResponse
import breez_sdk.OpenChannelFeeResponse
import breez_sdk.Payment
import breez_sdk.PaymentDetails
import breez_sdk.PaymentStatus
import breez_sdk.PaymentType
import breez_sdk.PrepareRedeemOnchainFundsResponse
import breez_sdk.PrepareRefundResponse
import breez_sdk.RecommendedFees
import breez_sdk.RedeemOnchainFundsResponse
import breez_sdk.RefundResponse
import breez_sdk.ReverseSwapInfo
import breez_sdk.ReverseSwapPairInfo
import breez_sdk.ReverseSwapStatus
import breez_sdk.SendPaymentResponse
import breez_sdk.SuccessActionProcessed
import breez_sdk.SwapInfo

internal fun NodeState.toLightningNodeState() = LightningNodeState(
    id = id,
    blockHeight = blockHeight,
    channelsBalanceMsat = channelsBalanceMsat,
    onchainBalanceMsat = onchainBalanceMsat,
    pendingOnchainBalanceMsat = pendingOnchainBalanceMsat,
    maxPayableMsat = maxPayableMsat,
    maxReceivableMsat = maxReceivableMsat,
    maxSinglePaymentAmountMsat = maxSinglePaymentAmountMsat,
    maxChanReserveMsats = maxChanReserveMsats,
    maxReceivableSinglePaymentAmountMsat = maxReceivableSinglePaymentAmountMsat,
    totalInboundLiquidityMsats = totalInboundLiquidityMsats,
    connectedPeers = connectedPeers,
)

internal fun HealthCheckStatus.toLightningHealthStatus() = when (this) {
    HealthCheckStatus.OPERATIONAL -> LightningHealthStatus.OPERATIONAL
    HealthCheckStatus.MAINTENANCE -> LightningHealthStatus.MAINTENANCE
    HealthCheckStatus.SERVICE_DISRUPTION -> LightningHealthStatus.SERVICE_DISRUPTION
}

private fun Payment.receivedSatoshi() = amountMsat.toLong() / 1000

internal fun BreezEvent.toLightningEvent(): LightningEvent? = when (this) {
    is BreezEvent.Synced -> LightningEvent.Synced
    is BreezEvent.NewBlock -> LightningEvent.NewBlock(block)
    is BreezEvent.InvoicePaid -> LightningEvent.InvoicePaid(
        paymentHash = details.paymentHash,
        paymentAmountSatoshi = details.payment?.receivedSatoshi()
    )

    else -> null
}

internal fun SwapInfo.toLightningSwapInfo() = LightningSwapInfo(
    bitcoinAddress = bitcoinAddress,
    bolt11 = bolt11,
    refundTxIds = refundTxIds,
    confirmedTxIds = confirmedTxIds,
    unconfirmedTxIds = unconfirmedTxIds,
    paymentHash = paymentHash.toUByteArray().toByteArray(),
    confirmedSats = confirmedSats,
    unconfirmedSats = unconfirmedSats,
    createdAt = createdAt,
    minAllowedDeposit = minAllowedDeposit,
    maxAllowedDeposit = maxAllowedDeposit,
    channelOpeningFees = channelOpeningFees?.let { LightningOpeningFeeParams(it.minMsat, it) }
)

internal fun ReverseSwapStatus.toLightningReverseSwapStatus() = when (this) {
    ReverseSwapStatus.INITIAL -> LightningReverseSwapStatus.INITIAL
    ReverseSwapStatus.IN_PROGRESS -> LightningReverseSwapStatus.IN_PROGRESS
    ReverseSwapStatus.CANCELLED -> LightningReverseSwapStatus.CANCELLED
    ReverseSwapStatus.COMPLETED_SEEN -> LightningReverseSwapStatus.COMPLETED_SEEN
    ReverseSwapStatus.COMPLETED_CONFIRMED -> LightningReverseSwapStatus.COMPLETED_CONFIRMED
}

internal fun ReverseSwapInfo.toLightningReverseSwapInfo() = LightningReverseSwapInfo(
    id = id,
    claimTxid = claimTxid,
    lockupTxid = lockupTxid,
    onchainAmountSat = onchainAmountSat,
    status = status.toLightningReverseSwapStatus(),
)

internal fun InputType.toLightningInputType(): LightningInputType? = when (this) {
    is InputType.Bolt11 -> LightningInputType.Bolt11(invoice.toLightningInvoice())
    is InputType.LnUrlPay -> LightningInputType.LnUrlPay(
        LnUrlPayData(
            domain = data.domain,
            minSendable = data.minSendable,
            maxSendable = data.maxSendable,
            metadataStr = data.metadataStr,
            raw = data,
        )
    )

    is InputType.LnUrlWithdraw -> LightningInputType.LnUrlWithdraw(
        LnUrlWithdrawData(
            callback = data.callback,
            k1 = data.k1,
            defaultDescription = data.defaultDescription,
            minWithdrawable = data.minWithdrawable,
            maxWithdrawable = data.maxWithdrawable,
        )
    )

    is InputType.LnUrlAuth -> LightningInputType.LnUrlAuth(
        LnUrlAuthData(k1 = data.k1, domain = data.domain, url = data.url, action = data.action)
    )

    else -> null
}

internal fun breez_sdk.LnInvoice.toLightningInvoice() = LightningInvoice(
    bolt11 = bolt11,
    amountSatoshi = amountMsat?.satoshi(),
    timestamp = timestamp.toLong(),
    expiry = expiry.toLong(),
    paymentHash = paymentHash,
    description = description,
)

internal fun SuccessActionProcessed.toLightningSuccessAction(): LightningSuccessAction? = when (this) {
    is SuccessActionProcessed.Message -> LightningSuccessAction.Message(data.message)
    is SuccessActionProcessed.Url -> LightningSuccessAction.Url(data.description, data.url)
    is SuccessActionProcessed.Aes -> (result as? AesSuccessActionDataResult.Decrypted)?.let {
        LightningSuccessAction.Aes(it.data.description, it.data.plaintext)
    }

    else -> null
}

internal fun ChannelState.toLightningChannelState() = when (this) {
    ChannelState.PENDING_OPEN -> LightningChannelState.PENDING_OPEN
    ChannelState.OPENED -> LightningChannelState.OPENED
    ChannelState.PENDING_CLOSE -> LightningChannelState.PENDING_CLOSE
    ChannelState.CLOSED -> LightningChannelState.CLOSED
}

internal fun Payment.toLightningPayment(): LightningPayment {
    val lightningDetails: LightningPaymentDetails = when (val d = details) {
        is PaymentDetails.Ln -> LightningPaymentDetails.Ln(
            destinationPubkey = d.data.destinationPubkey,
            paymentHash = d.data.paymentHash,
            paymentPreimage = d.data.paymentPreimage,
            bolt11 = d.data.bolt11,
            successAction = d.data.lnurlSuccessAction?.toLightningSuccessAction(),
        )

        is PaymentDetails.ClosedChannel -> LightningPaymentDetails.ClosedChannel(
            fundingTxid = d.data.fundingTxid,
            closingTxid = d.data.closingTxid,
            state = d.data.state.toLightningChannelState(),
        )
    }
    return LightningPayment(
        id = id,
        paymentType = when (paymentType) {
            PaymentType.RECEIVED -> LightningPaymentType.RECEIVED
            PaymentType.SENT -> LightningPaymentType.SENT
            PaymentType.CLOSED_CHANNEL -> LightningPaymentType.CLOSED_CHANNEL
        },
        paymentTime = paymentTime,
        amountMsat = amountMsat,
        feeMsat = feeMsat,
        status = when (status) {
            PaymentStatus.PENDING -> LightningPaymentStatus.PENDING
            PaymentStatus.COMPLETE -> LightningPaymentStatus.COMPLETE
            PaymentStatus.FAILED -> LightningPaymentStatus.FAILED
        },
        description = description,
        details = lightningDetails,
    )
}

internal fun RecommendedFees.toLightningFees() = LightningFees(
    fastestFee = fastestFee,
    halfHourFee = halfHourFee,
    hourFee = hourFee,
    economyFee = economyFee,
    minimumFee = minimumFee,
)

internal fun OpenChannelFeeResponse.toChannelOpenFee() = ChannelOpenFee(
    feeMsat = feeMsat,
    openingFeeParams = feeParams.let { LightningOpeningFeeParams(it.minMsat, it) },
)

internal fun OnchainPaymentLimitsResponse.toOnchainLimits() = OnchainLimits(maxPayableSat)

internal fun ReverseSwapPairInfo.toReverseSwapLimits() = ReverseSwapLimits(min, max, totalFees)

internal fun PrepareRefundResponse.toPrepareRefundResult() = PrepareRefundResult(refundTxFeeSat)

internal fun PrepareRedeemOnchainFundsResponse.toPrepareRedeemOnchainResult() =
    PrepareRedeemOnchainResult(txFeeSat)

internal fun RefundResponse.toRefundResult() = RefundResult(refundTxId)

internal fun RedeemOnchainFundsResponse.toRedeemOnchainResult() = RedeemOnchainResult(txid)

internal fun SendPaymentResponse.toSendPaymentResult() = SendPaymentResult(payment.id)

internal fun LnUrlPayResult.toLnUrlPayOutcome(): LnUrlPayOutcome = when (this) {
    is LnUrlPayResult.EndpointSuccess -> LnUrlPayOutcome.Success(
        message = data.successAction?.let {
            when (it) {
                is SuccessActionProcessed.Aes -> (it.result as? AesSuccessActionDataResult.Decrypted)
                    ?.let { d -> "${d.data.description}\n\n${d.data.plaintext}" } ?: ""

                is SuccessActionProcessed.Message -> it.data.message
                is SuccessActionProcessed.Url -> it.data.description
                else -> null
            }
        },
        url = (data.successAction as? SuccessActionProcessed.Url)?.data?.url,
    )

    is LnUrlPayResult.EndpointError -> LnUrlPayOutcome.Error(data.reason)
    is LnUrlPayResult.PayError -> LnUrlPayOutcome.PayError(data.reason, data.paymentHash)
}

internal fun LnUrlCallbackStatus.toLnUrlAuthOutcome(): LnUrlAuthOutcome = when (this) {
    is LnUrlCallbackStatus.Ok -> LnUrlAuthOutcome.Ok
    is LnUrlCallbackStatus.ErrorStatus -> LnUrlAuthOutcome.Error(data.reason)
}

internal fun LnUrlWithdrawResult.toLnUrlWithdrawOutcome(): LnUrlWithdrawOutcome = when (this) {
    is LnUrlWithdrawResult.Ok -> LnUrlWithdrawOutcome.Ok
    is LnUrlWithdrawResult.ErrorStatus -> LnUrlWithdrawOutcome.Error(data.reason)
    else -> LnUrlWithdrawOutcome.Error(toString())
}

