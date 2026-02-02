@file:OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)

package com.blockstream.data.lightning

import breez_sdk.GreenlightCredentials
import com.blockstream.data.BTC_POLICY_ASSET
import com.blockstream.data.data.FeePriority
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.Addressee
import com.blockstream.data.gdk.data.InputOutput
import com.blockstream.data.gdk.data.Output
import com.blockstream.data.gdk.data.Transaction
import com.blockstream.data.utils.hostname
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import kotlinx.serialization.json.Json
import lwk.Bolt11Invoice
import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlin.time.Instant

fun Long.milliSatoshi(): ULong = (this * 1000).toULong()

fun ULong.satoshi() = toLong() / 1000

fun ChannelOpenFee.feeSatoshi() = feeMsat?.satoshi()

fun LightningReceivePayment.receiveAmountSatoshi() = invoice.receiveAmountSatoshi(openingFeeSatoshi)

fun LightningInvoice.receiveAmountSatoshi(openingFeeSatoshi: Long) =
    (this.amountSatoshi ?: 0L) - openingFeeSatoshi

fun Bolt11Invoice.expireIn() = Instant.fromEpochSeconds((this.timestamp() + this.expiryTime()).toLong())
fun Bolt11Invoice.timeUntilExpiration() = expireIn().periodUntil(Clock.System.now(), TimeZone.currentSystemDefault())

fun LightningInvoice.expireIn() = Instant.fromEpochSeconds((this.timestamp + this.expiry).toLong())
fun LightningInvoice.timeUntilExpiration() = expireIn().periodUntil(Clock.System.now(), TimeZone.currentSystemDefault())

fun LightningInvoice.isExpired(): Boolean {
    return Clock.System.now() > expireIn()
}

fun LightningInvoice.isAmountLocked() = this.amountSatoshi != null

fun LightningInvoice.sendableSatoshi(userSatoshi: Long?): Long? {
    return if (isAmountLocked()) {
        this.amountSatoshi ?: 0L
    } else {
        userSatoshi
    }
}

fun LnUrlWithdrawData.maxWithdrawableSatoshi() = this.maxWithdrawable.satoshi()
fun LnUrlWithdrawData.minWithdrawableSatoshi() = this.minWithdrawable.satoshi()

fun LnUrlWithdrawData.domain() = this.callback.hostname(excludePort = true)

fun LnUrlPayData.isAmountLocked() = minSendable == maxSendable
fun LnUrlPayData.sendableSatoshi(userSatoshi: Long?): Long? {
    return if (isAmountLocked()) {
        maxSendableSatoshi()
    } else {
        userSatoshi
    }
}

fun LnUrlPayData.maxSendableSatoshi() = this.maxSendable.satoshi()
fun LnUrlPayData.minSendableSatoshi() = this.minSendable.satoshi()

fun LnUrlPayData.metadata(): List<List<String>>? {
    return try {
        Json.decodeFromString<List<List<String>>>(metadataStr)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun List<List<String>>?.lnUrlPayDescription(): String? {
    return this?.find {
        "text/plain" == it.getOrNull(0)
    }?.last()
}

fun List<List<String>>?.lnUrlPayImage(): ByteArray? {
    return this?.find {
        "image/png;base64" == it.getOrNull(0)
    }?.last()?.let {
        try {
            Base64.decode(it)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

fun LightningNodeState.isLoading() = this.id.isBlank()
fun LightningNodeState.channelsBalanceSatoshi() = this.channelsBalanceMsat.satoshi()
fun LightningNodeState.onchainBalanceSatoshi() = this.onchainBalanceMsat.satoshi()
fun LightningNodeState.maxReceivableSatoshi() = this.maxReceivableMsat.satoshi()
fun LightningNodeState.totalInboundLiquiditySatoshi() = this.totalInboundLiquidityMsats.satoshi()
fun LightningNodeState.maxSinglePaymentAmountSatoshi() = this.maxSinglePaymentAmountMsat.satoshi()
fun LightningNodeState.maxPayableSatoshi() = this.maxPayableMsat.satoshi()

fun LightningPayment.amountSatoshi() = amountMsat.satoshi() * if (paymentType == LightningPaymentType.RECEIVED) 1 else -1

fun Addressee.Companion.fromInvoice(invoice: LightningInvoice, fallbackAmount: Long): Addressee {
    return Addressee(
        address = invoice.bolt11,
        satoshi = (invoice.amountSatoshi ?: fallbackAmount).let { -it },
        isInvoiceAmountLocked = invoice.amountSatoshi != null,
    )
}

fun Addressee.Companion.fromLnUrlPay(requestData: LnUrlPayData, input: String, satoshi: Long): Addressee {
    return Addressee(
        address = input,
        satoshi = -(requestData.sendableSatoshi(satoshi) ?: 0),
        domain = requestData.domain,
        metadata = requestData.metadata(),
        isInvoiceAmountLocked = requestData.isAmountLocked(),
        minAmount = requestData.minSendableSatoshi(),
        maxAmount = requestData.maxSendableSatoshi()
    )
}

fun Output.Companion.fromInvoice(invoice: LightningInvoice, fallbackAmount: Long): Output {
    return Output(
        address = invoice.bolt11,
        satoshi = (invoice.amountSatoshi ?: fallbackAmount).let { -it },
        isChange = false
    )
}

fun Output.Companion.fromLnUrlPay(requestData: LnUrlPayData, input: String, satoshi: Long): Output {
    return Output(
        address = input,
        satoshi = -(requestData.sendableSatoshi(satoshi) ?: 0),
        domain = requestData.domain,
        isChange = false
    )
}

fun Transaction.Companion.fromPayment(payment: LightningPayment): Transaction {
    val extras = buildMap {
        when (val details = payment.details) {
            is LightningPaymentDetails.ClosedChannel -> {
                details.fundingTxid.also { put("id_funding_transaction_id", it) }
                details.closingTxid?.also { put("id_closing_transaction_id", it) }
            }

            is LightningPaymentDetails.Ln -> {
                payment.description.takeIf { it?.isNotBlank() == true }?.also { put("id_invoice_description", it) }
                put("id_destination_public_key", details.destinationPubkey)
                put("id_payment_hash", details.paymentHash)
                put("id_payment_preimage", details.paymentPreimage)
                put("id_invoice", details.bolt11)
            }
        }
    }.toList()

    val isPendingCloseChannel =
        payment.paymentType == LightningPaymentType.CLOSED_CHANNEL &&
                (payment.details as? LightningPaymentDetails.ClosedChannel)?.state == LightningChannelState.PENDING_CLOSE

    val blockHeight = when {
        isPendingCloseChannel || payment.status == LightningPaymentStatus.PENDING -> 0
        payment.status == LightningPaymentStatus.COMPLETE -> payment.paymentTime
        else -> 0
    }

    val lnDetails = payment.details as? LightningPaymentDetails.Ln
    val closedDetails = payment.details as? LightningPaymentDetails.ClosedChannel

    return Transaction(
        blockHeight = blockHeight,
        canRBF = false,
        createdAtTs = payment.paymentTime * 1_000_000,
        inputs = listOf(),
        outputs = listOf(
            InputOutput(
                satoshi = payment.amountSatoshi(),
            )
        ),
        fee = payment.feeMsat.satoshi(),
        feeRate = 0,
        memo = payment.description ?: "",
        spvVerified = "",
        txHash = lnDetails?.paymentHash ?: closedDetails?.closingTxid ?: payment.id,
        type = if (payment.paymentType == LightningPaymentType.RECEIVED) "incoming" else "outgoing",
        satoshi = mapOf(BTC_POLICY_ASSET to (payment.amountSatoshi() - if (payment.paymentType == LightningPaymentType.SENT) payment.feeMsat.satoshi() else 0L)),
        message = (lnDetails?.successAction as? LightningSuccessAction.Message)?.message,
        plaintext = (lnDetails?.successAction as? LightningSuccessAction.Aes)?.let { it.description to it.plaintext },
        url = (lnDetails?.successAction as? LightningSuccessAction.Url)?.let { it.description to it.url },
        isCloseChannel = payment.paymentType == LightningPaymentType.CLOSED_CHANNEL,
        isPendingCloseChannel = isPendingCloseChannel,
        extras = extras
    )
}

fun Transaction.Companion.fromSwapInfo(account: Account, swapInfo: LightningSwapInfo, isRefundableSwap: Boolean): Transaction {
    val extras = buildMap {
        swapInfo.bolt11?.let {
            put("id_invoice", it)
        }
    }.toList()

    return Transaction(
        accountInjected = account,
        blockHeight = if (isRefundableSwap) Long.MAX_VALUE else 0,
        canRBF = false,
        createdAtTs = swapInfo.createdAt.let { if (it > 0) it * 1_000_000 else 0 },
        inputs = listOf(
            InputOutput(
                address = swapInfo.bitcoinAddress
            )
        ),
        outputs = listOf(),
        fee = 0,
        feeRate = 0,
        memo = "",
        spvVerified = "",
        txHash = swapInfo.refundTxIds.firstOrNull() ?: swapInfo.confirmedTxIds.firstOrNull()
        ?: swapInfo.unconfirmedTxIds.firstOrNull() ?: swapInfo.paymentHash
            .toHexString(),
        type = Transaction.Type.IN.gdkType,
        satoshi = mapOf(BTC_POLICY_ASSET to swapInfo.confirmedSats.toLong() + (if (isRefundableSwap) 0 else swapInfo.unconfirmedSats.toLong())),
        isLightningSwap = true,
        isInProgressSwap = swapInfo.confirmedSats.toLong() > 0 && !isRefundableSwap,
        isRefundableSwap = isRefundableSwap,
        extras = extras
    )
}

fun Transaction.Companion.fromReverseSwapInfo(account: Account, reverseSwapInfo: LightningReverseSwapInfo): Transaction {
    return Transaction(
        accountInjected = account,
        blockHeight = 0,
        canRBF = false,
        createdAtTs = 0,
        inputs = listOf(),
        outputs = listOf(),
        fee = 0,
        feeRate = 0,
        memo = "",
        spvVerified = "",
        txHash = reverseSwapInfo.claimTxid ?: reverseSwapInfo.lockupTxid ?: reverseSwapInfo.id,
        type = Transaction.Type.OUT.gdkType,
        satoshi = mapOf(BTC_POLICY_ASSET to reverseSwapInfo.onchainAmountSat.toLong()),
        isLightningSwap = true,
        isInProgressSwap = true,
    )
}

fun AppGreenlightCredentials.Companion.fromGreenlightCredentials(greenlightCredentials: GreenlightCredentials): AppGreenlightCredentials {
    return AppGreenlightCredentials(
        deviceKey = greenlightCredentials.developerKey,
        deviceCert = greenlightCredentials.developerCert
    )
}

fun LightningFees.fee(feePriority: FeePriority): Long {
    return feePriority.let {
        when (it) {
            is FeePriority.High -> fastestFee
            is FeePriority.Medium -> hourFee
            is FeePriority.Low -> economyFee
            is FeePriority.Custom -> it.customFeeRate.toULong().coerceAtLeast(minimumFee)
        }
    }.toLong()
}
