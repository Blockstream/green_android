package com.blockstream.common.lightning

import breez_sdk.GreenlightCredentials
import breez_sdk.LnInvoice
import breez_sdk.LnUrlPayRequestData
import breez_sdk.LnUrlWithdrawRequestData
import breez_sdk.LspInformation
import breez_sdk.NodeState
import breez_sdk.Payment
import breez_sdk.PaymentDetails
import breez_sdk.PaymentType
import breez_sdk.SuccessActionProcessed
import breez_sdk.SwapInfo
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.gdk.data.Address
import com.blockstream.common.gdk.data.Addressee
import com.blockstream.common.gdk.data.InputOutput
import com.blockstream.common.gdk.data.Output
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.utils.hostname
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64


fun LnInvoice.amountSatoshi() = this.amountMsat?.let { it.toLong() / 1000 }

fun LnInvoice.expireIn() = Instant.fromEpochSeconds((this.timestamp + this.expiry).toLong())
fun LnInvoice.timeUntilExpiration() = expireIn().periodUntil(Clock.System.now(), TimeZone.currentSystemDefault())

fun LnInvoice.isExpired(): Boolean {
    return Clock.System.now() > expireIn()
}

fun LnInvoice.isAmountLocked() = this.amountMsat != null

fun LnInvoice.sendableSatoshi(userSatoshi: Long?): Long? {
    return if(isAmountLocked()){
        this.amountSatoshi() ?: 0L
    } else {
        userSatoshi
    }
}

fun LnUrlWithdrawRequestData.maxWithdrawableSatoshi() = this.maxWithdrawable.toLong() / 1000
fun LnUrlWithdrawRequestData.minWithdrawableSatoshi() = this.minWithdrawable.toLong() / 1000

fun LnUrlWithdrawRequestData.domain() = this.callback.hostname(excludePort = true)

fun LnUrlPayRequestData.isAmountLocked() = minSendable == maxSendable
fun LnUrlPayRequestData.sendableSatoshi(userSatoshi: Long?): Long? {
    return if(isAmountLocked()){
        maxSendableSatoshi()
    }else{
        userSatoshi
    }
}

fun LnUrlPayRequestData.maxSendableSatoshi() = this.maxSendable.toLong() / 1000
fun LnUrlPayRequestData.minSendableSatoshi() = this.minSendable.toLong() / 1000

fun LnUrlPayRequestData.metadata(): List<List<String>>? {
    return try{
        Json.decodeFromString<List<List<String>>>(metadataStr)
    }catch (e: Exception){
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
        try{
            Base64.decode(it)
        }catch (e: Exception){
            e.printStackTrace()
            null
        }
    }
}

fun NodeState.isLoading() = this.id.isBlank()
fun NodeState.channelsBalanceSatoshi() = this.channelsBalanceMsat.toLong() / 1000
fun NodeState.onchainBalanceSatoshi() = this.onchainBalanceMsat.toLong() / 1000
fun NodeState.maxReceivableSatoshi() = this.maxReceivableMsat.toLong() / 1000
fun NodeState.inboundLiquiditySatoshi() = this.inboundLiquidityMsats.toLong() / 1000
fun NodeState.maxSinglePaymentAmountSatoshi() = this.maxSinglePaymentAmountMsat.toLong() / 1000
fun NodeState.maxPayableSatoshi() = this.maxPayableMsat.toLong() / 1000

fun Payment.amountSatoshi() = (this.amountMsat.toLong() / 1000) * if(paymentType == PaymentType.RECEIVED) 1 else -1

fun LspInformation.channelMinimumFeeSatoshi() = this.channelMinimumFeeMsat / 1000
fun LspInformation.channelFeePercent() = this.channelFeePermyriad / 100.0

fun Addressee.Companion.fromInvoice(invoice: LnInvoice, fallbackAmount: Long): Addressee {
    return Addressee(
        address = invoice.bolt11,
        satoshi = (invoice.amountSatoshi() ?: fallbackAmount).let { -it },
        hasLockedAmount = invoice.amountMsat != null,
    )
}

fun Addressee.Companion.fromLnUrlPay(requestData: LnUrlPayRequestData, input: String, satoshi: Long): Addressee {
    return Addressee(
        address = input,
        satoshi = -(requestData.sendableSatoshi(satoshi) ?: 0),
        domain = requestData.domain,
        metadata = requestData.metadata(),
        hasLockedAmount = requestData.isAmountLocked(),
        minAmount = requestData.minSendableSatoshi(),
        maxAmount = requestData.maxSendableSatoshi()
    )
}

fun Output.Companion.fromInvoice(invoice: LnInvoice, fallbackAmount: Long): Output {
    return Output(
        address = invoice.bolt11,
        satoshi = (invoice.amountSatoshi() ?: fallbackAmount).let { -it },
        isChange = false
    )
}
fun Output.Companion.fromLnUrlPay(requestData: LnUrlPayRequestData, input: String, satoshi: Long): Output {
    return Output(
        address = input,
        satoshi = -(requestData.sendableSatoshi(satoshi) ?: 0),
        domain = requestData.domain,
        isChange = false
    )
}

fun Transaction.Companion.fromPayment(payment: Payment): Transaction {
    return Transaction(
        blockHeight = payment.paymentTime,
        canCPFP = false,
        canRBF = false,
        createdAtTs = payment.paymentTime * 1_000_000,
        inputs = listOf(),
        outputs = listOf(),
        fee = payment.feeMsat.toLong() / 1000,
        feeRate = 0,
        memo = payment.description ?: "",
        rbfOptin = false,
        spvVerified = "",
        txHash = payment.id,
        type = if(payment.paymentType == PaymentType.RECEIVED) "incoming" else "outgoing",
        satoshi = mapOf(BTC_POLICY_ASSET to payment.amountSatoshi()),
        message = ((payment.details as? PaymentDetails.Ln)?.data?.lnurlSuccessAction as? SuccessActionProcessed.Message)?.data?.message,
        plaintext = ((payment.details as? PaymentDetails.Ln)?.data?.lnurlSuccessAction as? SuccessActionProcessed.Aes)?.data?.let { it.description to it.plaintext },
        url = ((payment.details as? PaymentDetails.Ln)?.data?.lnurlSuccessAction as? SuccessActionProcessed.Url)?.data?.let { it.description to it.url},
        isPendingCloseChannel = payment.paymentType == PaymentType.CLOSED_CHANNEL && payment.pending
    )
}

fun Transaction.Companion.fromSwapInfo(account: Account, swapInfo: SwapInfo, isRefundableSwap: Boolean): Transaction {
    return Transaction(
        accountInjected = account,
        blockHeight = if(isRefundableSwap) Long.MAX_VALUE else 0,
        canCPFP = false,
        canRBF = false,
        createdAtTs = swapInfo.createdAt,
        inputs = listOf(
            InputOutput(
                address = swapInfo.bitcoinAddress
            )
        ),
        outputs = listOf(),
        fee = 0,
        feeRate = 0,
        memo = "",
        rbfOptin = false,
        spvVerified = "",
        txHash = swapInfo.paymentHash.toString(),
        type = Transaction.Type.MIXED.gdkType,
        satoshi = mapOf(BTC_POLICY_ASSET to swapInfo.confirmedSats.toLong()),
        isLightningSwap = true,
        isInProgressSwap = swapInfo.confirmedSats.toLong() > 0 && !isRefundableSwap,
        isRefundableSwap = isRefundableSwap
    )
}

fun Address.Companion.fromInvoice(invoice: LnInvoice): Address {
    return Address(address = invoice.bolt11)
}

fun Address.Companion.fromSwapInfo(swapInfo: SwapInfo): Address {
    return Address(address = swapInfo.bitcoinAddress)
}

fun AppGreenlightCredentials.Companion.fromGreenlightCredentials(greenlightCredentials: GreenlightCredentials): AppGreenlightCredentials {
    return AppGreenlightCredentials(
        deviceKey = greenlightCredentials.deviceKey,
        deviceCert = greenlightCredentials.deviceCert
    )
}

fun AppGreenlightCredentials.toGreenlightCredentials(): GreenlightCredentials {
    return GreenlightCredentials(
        deviceKey = deviceKey,
        deviceCert = deviceCert
    )
}