/**
 * Conversion functions from glsdk types to LightningTypes DTOs.
 * Mirrors BreezMappers.kt (which handles breez_sdk types).
 */
@file:OptIn(ExperimentalStdlibApi::class)

package com.blockstream.data.lightning

import com.blockstream.glsdk.LnUrlPayRequestData
import com.blockstream.glsdk.LnUrlPayResult
import com.blockstream.glsdk.LnUrlWithdrawRequestData
import com.blockstream.glsdk.LnUrlWithdrawResult
import com.blockstream.glsdk.ResolvedInput
import com.blockstream.glsdk.NodeEvent
import com.blockstream.glsdk.ParsedInvoice
import com.blockstream.glsdk.Payment
import com.blockstream.glsdk.PaymentType
import com.blockstream.glsdk.PaymentStatus
import com.blockstream.glsdk.SendResponse
import com.blockstream.glsdk.SuccessActionProcessed
import io.ktor.http.Url

internal fun Payment.toLightningPayment() = LightningPayment(
    id = id,
    paymentType = when (paymentType) {
        PaymentType.RECEIVED -> LightningPaymentType.RECEIVED
        PaymentType.SENT -> LightningPaymentType.SENT
    },
    paymentTime = paymentTime.toLong(),
    amountMsat = amountMsat,
    feeMsat = feeMsat,
    amountTotalMsat = amountMsat + if (paymentType == PaymentType.SENT) feeMsat else 0uL,
    status = when (status) {
        PaymentStatus.PENDING -> LightningPaymentStatus.PENDING
        PaymentStatus.COMPLETE -> LightningPaymentStatus.COMPLETE
        PaymentStatus.FAILED -> LightningPaymentStatus.FAILED
    },
    description = description,
    details = LightningPaymentDetails.Ln(
        destinationPubkey = destination,
        paymentHash = id,
        paymentPreimage = preimage ?: "",
        bolt11 = bolt11 ?: "",
        successAction = null,
    ),
)

internal fun SendResponse.toSendPaymentResult() = SendPaymentResult(paymentHash)

internal fun ParsedInvoice.toLightningInvoice() = LightningInvoice(
    bolt11 = bolt11,
    amountSatoshi = amountMsat?.satoshi(),
    timestamp = timestamp.toLong(),
    expiry = expiry.toLong(),
    paymentHash = paymentHash,
    description = description,
)

internal fun ResolvedInput.toLightningInputType(): LightningInputType? = when (this) {
    is ResolvedInput.Bolt11 -> LightningInputType.Bolt11(invoice.toLightningInvoice())
    is ResolvedInput.NodeId -> null
    is ResolvedInput.LnUrlPay -> LightningInputType.LnUrlPay(data.toLightningLnUrlPayData())
    is ResolvedInput.LnUrlWithdraw -> LightningInputType.LnUrlWithdraw(data.toLightningLnUrlWithdrawData())
}

//internal fun OnchainFeeRates.toLightningFees() = LightningFees(
//    fastestFee = nextBlockSatPerVbyte,
//    halfHourFee = halfHourSatPerVbyte,
//    hourFee = hourSatPerVbyte,
//    economyFee = daySatPerVbyte,
//    minimumFee = minimumRelaySatPerVbyte,
//)

// glsdk's LnUrlPayRequestData exposes fewer fields than breez's — `domain` is derived from `callback`.
internal fun LnUrlPayRequestData.toLightningLnUrlPayData() = LnUrlPayData(
    domain = Url(callback).host,
    minSendable = minSendable,
    maxSendable = maxSendable,
    metadataStr = metadata,
    raw = this,
)

internal fun LnUrlWithdrawRequestData.toLightningLnUrlWithdrawData() = LnUrlWithdrawData(
    callback = callback,
    k1 = k1,
    defaultDescription = defaultDescription,
    minWithdrawable = minWithdrawable,
    maxWithdrawable = maxWithdrawable,
)

internal fun SuccessActionProcessed.toLightningSuccessAction(): LightningSuccessAction = when (this) {
    is SuccessActionProcessed.Message -> LightningSuccessAction.Message(message)
    is SuccessActionProcessed.Url -> LightningSuccessAction.Url(description, url)
    is SuccessActionProcessed.Aes -> LightningSuccessAction.Aes(description, plaintext)
}

internal fun LnUrlPayResult.toLnUrlPayOutcome(): LnUrlPayOutcome = when (this) {
    is LnUrlPayResult.EndpointSuccess -> {
        val action = data.successAction?.toLightningSuccessAction()
        LnUrlPayOutcome.Success(
            message = when (action) {
                is LightningSuccessAction.Message -> action.message
                is LightningSuccessAction.Url -> action.description
                is LightningSuccessAction.Aes -> "${action.description}\n\n${action.plaintext}"
                null -> null
            },
            url = (action as? LightningSuccessAction.Url)?.url,
        )
    }
    is LnUrlPayResult.EndpointError -> LnUrlPayOutcome.Error(data.reason)
    is LnUrlPayResult.PayError -> LnUrlPayOutcome.PayError(data.reason, data.paymentHash)
}

internal fun LnUrlWithdrawResult.toLnUrlWithdrawOutcome(): LnUrlWithdrawOutcome = when (this) {
    is LnUrlWithdrawResult.Ok -> LnUrlWithdrawOutcome.Ok
    is LnUrlWithdrawResult.ErrorStatus -> LnUrlWithdrawOutcome.Error(data.reason)
}

internal fun com.blockstream.glsdk.NodeState.toLightningNodeState() = LightningNodeState(
    id = id,
    blockHeight = blockHeight,
    channelsBalanceMsat = channelsBalanceMsat,
    onchainBalanceMsat = onchainBalanceMsat,
    totalInboundLiquidityMsats = totalInboundLiquidityMsat,
    pendingOnchainBalanceMsat = pendingOnchainBalanceMsat,
    maxPayableMsat = maxPayableMsat,
    maxReceivableMsat = totalInboundLiquidityMsat,
    maxSinglePaymentAmountMsat = maxPayableMsat,
    maxChanReserveMsats = 0uL,
    maxReceivableSinglePaymentAmountMsat = maxReceivableSinglePaymentMsat,
    connectedPeers = connectedChannelPeers,
)

internal fun NodeEvent.toLightningEvent(): LightningEvent = when (this) {
    is NodeEvent.InvoicePaid -> LightningEvent.InvoicePaid(
        paymentHash = details.paymentHash,
        paymentAmountSatoshi = details.amountMsat.satoshi()
    )
}