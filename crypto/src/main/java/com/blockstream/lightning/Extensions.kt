package com.blockstream.lightning

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import breez_sdk.LnInvoice
import breez_sdk.LnUrlPayRequestData
import breez_sdk.LnUrlWithdrawRequestData
import breez_sdk.LspInformation
import breez_sdk.NodeState
import breez_sdk.Payment
import breez_sdk.PaymentType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import kotlinx.serialization.json.Json
import java.net.URI
import java.util.Date


fun LnInvoice.amountSatoshi() = this.amountMsat?.let { it.toLong() / 1000 }

fun LnInvoice.expireIn() = Instant.fromEpochSeconds((this.timestamp + this.expiry).toLong())
fun LnInvoice.expireInAsDate() = Date(expireIn().toEpochMilliseconds())

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

fun LnUrlWithdrawRequestData.domain(): String? {
    return URI(this.callback).host
}

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

fun List<List<String>>?.lnUrlPayImage(): Bitmap? {
    return this?.find {
        "image/png;base64" == it.getOrNull(0)
    }?.last()?.let {
        try{
            val decodedString = Base64.decode(it, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        }catch (e: Exception){
            e.printStackTrace()
            null
        }
    }
}

fun NodeState.isLoading() = this.id.isBlank()
fun NodeState.channelsBalanceSatoshi() = this.channelsBalanceMsat.toLong() / 1000
fun NodeState.maxReceivableSatoshi() = this.maxReceivableMsat.toLong() / 1000
fun NodeState.inboundLiquiditySatoshi() = this.inboundLiquidityMsats.toLong() / 1000
fun NodeState.maxSinglePaymentAmountSatoshi() = this.maxSinglePaymentAmountMsat.toLong() / 1000
fun NodeState.maxPayableSatoshi() = this.maxPayableMsat.toLong() / 1000

fun Payment.amountSatoshi() = (this.amountMsat.toLong() / 1000) * if(paymentType == PaymentType.RECEIVED) 1 else -1

fun LspInformation.channelMinimumFeeSatoshi() = this.channelMinimumFeeMsat / 1000
fun LspInformation.channelFeePercent() = this.channelFeePermyriad / 100.0