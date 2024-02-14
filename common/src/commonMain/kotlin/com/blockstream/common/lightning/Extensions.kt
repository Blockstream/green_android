package com.blockstream.common.lightning

import breez_sdk.AesSuccessActionDataResult
import breez_sdk.GreenlightCredentials
import breez_sdk.LnInvoice
import breez_sdk.LnUrlPayRequestData
import breez_sdk.LnUrlWithdrawRequestData
import breez_sdk.NodeState
import breez_sdk.OpenChannelFeeResponse
import breez_sdk.OpeningFeeParams
import breez_sdk.Payment
import breez_sdk.PaymentDetails
import breez_sdk.PaymentStatus
import breez_sdk.PaymentType
import breez_sdk.ReceivePaymentResponse
import breez_sdk.ReverseSwapInfo
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

fun Long.milliSatoshi(): ULong = (this * 1000).toULong()

fun ULong.satoshi() = toLong() / 1000

fun OpenChannelFeeResponse.feeSatoshi() = feeMsat.satoshi()

fun ReceivePaymentResponse.receiveAmountSatoshi() = lnInvoice.receiveAmountSatoshi(openingFeeParams)

fun LnInvoice.amountSatoshi() = this.amountMsat?.satoshi()
fun LnInvoice.receiveAmountSatoshi(openingFeeParams: OpeningFeeParams?) =
    (this.amountMsat?.satoshi() ?: 0L) - (openingFeeParams?.minMsat?.satoshi() ?: 0L)

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

fun LnUrlWithdrawRequestData.maxWithdrawableSatoshi() = this.maxWithdrawable.satoshi()
fun LnUrlWithdrawRequestData.minWithdrawableSatoshi() = this.minWithdrawable.satoshi()

fun LnUrlWithdrawRequestData.domain() = this.callback.hostname(excludePort = true)

fun LnUrlPayRequestData.isAmountLocked() = minSendable == maxSendable
fun LnUrlPayRequestData.sendableSatoshi(userSatoshi: Long?): Long? {
    return if(isAmountLocked()){
        maxSendableSatoshi()
    }else{
        userSatoshi
    }
}

fun LnUrlPayRequestData.maxSendableSatoshi() = this.maxSendable.satoshi()
fun LnUrlPayRequestData.minSendableSatoshi() = this.minSendable.satoshi()

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
fun NodeState.channelsBalanceSatoshi() = this.channelsBalanceMsat.satoshi()
fun NodeState.onchainBalanceSatoshi() = this.onchainBalanceMsat.satoshi()
fun NodeState.maxReceivableSatoshi() = this.maxReceivableMsat.satoshi()
fun NodeState.inboundLiquiditySatoshi() = this.inboundLiquidityMsats.satoshi()
fun NodeState.maxSinglePaymentAmountSatoshi() = this.maxSinglePaymentAmountMsat.satoshi()
fun NodeState.maxPayableSatoshi() = this.maxPayableMsat.satoshi()

fun Payment.amountSatoshi() = amountMsat.satoshi() * if(paymentType == PaymentType.RECEIVED) 1 else -1

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
        fee = payment.feeMsat.satoshi(),
        feeRate = 0,
        memo = payment.description ?: "",
        rbfOptin = false,
        spvVerified = "",
        txHash = (payment.details as? PaymentDetails.Ln)?.data?.paymentHash ?: (payment.details as? PaymentDetails.ClosedChannel)?.data?.closingTxid ?: payment.id,
        type = if(payment.paymentType == PaymentType.RECEIVED) "incoming" else "outgoing",
        satoshi = mapOf(BTC_POLICY_ASSET to payment.amountSatoshi()),
        message = ((payment.details as? PaymentDetails.Ln)?.data?.lnurlSuccessAction as? SuccessActionProcessed.Message)?.data?.message,
        plaintext = (((payment.details as? PaymentDetails.Ln)?.data?.lnurlSuccessAction as? SuccessActionProcessed.Aes)?.result as? AesSuccessActionDataResult.Decrypted)?.data?.let { it.description to it.plaintext },
        url = ((payment.details as? PaymentDetails.Ln)?.data?.lnurlSuccessAction as? SuccessActionProcessed.Url)?.data?.let { it.description to it.url},
        isPendingCloseChannel = payment.paymentType == PaymentType.CLOSED_CHANNEL && payment.status == PaymentStatus.PENDING
    )
}

fun Transaction.Companion.fromSwapInfo(account: Account, swapInfo: SwapInfo, isRefundableSwap: Boolean): Transaction {
    return Transaction(
        accountInjected = account,
        blockHeight = if(isRefundableSwap) Long.MAX_VALUE else 0,
        canCPFP = false,
        canRBF = false,
        createdAtTs = swapInfo.createdAt.let { if(it > 0) it * 1_000_000 else 0 },
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
        txHash = swapInfo.refundTxIds.firstOrNull() ?: "",
        type = Transaction.Type.MIXED.gdkType,
        satoshi = mapOf(BTC_POLICY_ASSET to swapInfo.confirmedSats.toLong() + (if(isRefundableSwap) 0 else swapInfo.unconfirmedSats.toLong())),
        isLightningSwap = true,
        isInProgressSwap = swapInfo.confirmedSats.toLong() > 0 && !isRefundableSwap,
        isRefundableSwap = isRefundableSwap
    )
}

fun Transaction.Companion.fromReverseSwapInfo(account: Account, reverseSwapInfo: ReverseSwapInfo): Transaction {
    return Transaction(
        accountInjected = account,
        blockHeight = 0,
        canCPFP = false,
        canRBF = false,
        createdAtTs = 0,
        inputs = listOf(),
        outputs = listOf(),
        fee = 0,
        feeRate = 0,
        memo = "",
        rbfOptin = false,
        spvVerified = "",
        txHash = reverseSwapInfo.claimTxid ?: reverseSwapInfo.lockupTxid ?: reverseSwapInfo.id,
        type = Transaction.Type.OUT.gdkType,
        satoshi = mapOf(BTC_POLICY_ASSET to reverseSwapInfo.onchainAmountSat.toLong()),
        isLightningSwap = true,
        isInProgressSwap = true,
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