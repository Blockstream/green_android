package com.blockstream.data.lightning

import breez_sdk.BlockingBreezServices
import breez_sdk.LnUrlAuthRequestData
import breez_sdk.LnUrlPayRequest
import breez_sdk.ListPaymentsRequest
import breez_sdk.NodeState
import breez_sdk.OpenChannelFeeRequest
import breez_sdk.PayOnchainRequest
import breez_sdk.PrepareOnchainPaymentRequest
import breez_sdk.PrepareRedeemOnchainFundsRequest
import breez_sdk.PrepareRefundRequest
import breez_sdk.ReceiveOnchainRequest
import breez_sdk.ReceivePaymentRequest
import breez_sdk.RedeemOnchainFundsRequest
import breez_sdk.RefundRequest
import breez_sdk.ReverseSwapFeesRequest
import breez_sdk.ReverseSwapStatus
import breez_sdk.SendPaymentRequest
import breez_sdk.SwapAmountType
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.extensions.tryCatchNull
import com.blockstream.jade.Loggable

class BreezSdk(private val breezSdk: BlockingBreezServices) {

    fun listPayments() = breezSdk.listPayments(ListPaymentsRequest())?.map { it.toLightningPayment() }

    fun nodeInfo(): NodeState = breezSdk.nodeInfo()

    fun updateLspInformation() = breezSdk.lspId()?.let { breezSdk.fetchLspInfo(it) }

    fun recommendedFees(): LightningFees? = breezSdk.recommendedFees()?.toLightningFees()

    fun rescanSwaps() = breezSdk.rescanSwaps()

    fun openChannelFee(satoshi: Long): ChannelOpenFee =
        breezSdk.openChannelFee(OpenChannelFeeRequest(amountMsat = satoshi.toULong() * 1000u)).toChannelOpenFee()

    fun receiveOnchain(): LightningSwapInfo =
        breezSdk.receiveOnchain(ReceiveOnchainRequest()).toLightningSwapInfo()

    fun inProgressSwap(): LightningSwapInfo? = breezSdk.inProgressSwap()?.toLightningSwapInfo()

    fun listRefundables(): List<LightningSwapInfo> =
        breezSdk.listRefundables()?.map { it.toLightningSwapInfo() } ?: emptyList()

    fun inProgressOnchainPayments(): List<LightningReverseSwapInfo> =
        breezSdk.inProgressOnchainPayments()
            ?.filter { it.status == ReverseSwapStatus.INITIAL || it.status == ReverseSwapStatus.IN_PROGRESS }
            ?.map { it.toLightningReverseSwapInfo() }
            ?: emptyList()

    fun prepareRefund(swapAddress: String, toAddress: String, satPerVbyte: UInt?): PrepareRefundResult =
        breezSdk.prepareRefund(
            PrepareRefundRequest(
                swapAddress = swapAddress,
                toAddress = toAddress,
                satPerVbyte = satPerVbyte ?: breezSdk.recommendedFees().economyFee.toUInt()
            )
        ).toPrepareRefundResult()

    fun prepareRedeemOnchainFunds(toAddress: String, satPerVbyte: UInt?): PrepareRedeemOnchainResult =
        breezSdk.prepareRedeemOnchainFunds(
            PrepareRedeemOnchainFundsRequest(
                toAddress = toAddress,
                satPerVbyte = satPerVbyte ?: breezSdk.recommendedFees().economyFee.toUInt()
            )
        ).toPrepareRedeemOnchainResult()

    fun refund(swapAddress: String, toAddress: String, satPerVbyte: UInt?): RefundResult =
        breezSdk.refund(
            RefundRequest(
                swapAddress = swapAddress,
                toAddress = toAddress,
                satPerVbyte = satPerVbyte ?: breezSdk.recommendedFees().economyFee.toUInt()
            )
        ).toRefundResult()

    fun redeemOnchainFunds(toAddress: String, satPerVbyte: UInt?): RedeemOnchainResult =
        breezSdk.redeemOnchainFunds(
            RedeemOnchainFundsRequest(
                toAddress = toAddress,
                satPerVbyte = satPerVbyte ?: breezSdk.recommendedFees().economyFee.toUInt()
            )
        ).toRedeemOnchainResult()

    fun onchainPaymentLimits(): OnchainLimits = breezSdk.onchainPaymentLimits().toOnchainLimits()

    fun fetchReverseSwapFees(sendAmountSat: ULong? = null): ReverseSwapLimits =
        breezSdk.fetchReverseSwapFees(ReverseSwapFeesRequest(sendAmountSat)).toReverseSwapLimits()

    fun payOnchain(address: String, satPerVbyte: UInt?) {
        val amount = breezSdk.onchainPaymentLimits().maxPayableSat
        val prepareRes = breezSdk.prepareOnchainPayment(
            PrepareOnchainPaymentRequest(amount, SwapAmountType.SEND, satPerVbyte ?: 10.toUInt())
        )
        breezSdk.payOnchain(PayOnchainRequest(address, prepareRes))
    }

    fun sendPayment(invoice: LightningInvoice, satoshi: Long?): SendPaymentResult =
        breezSdk.sendPayment(
            SendPaymentRequest(bolt11 = invoice.bolt11, amountMsat = satoshi?.milliSatoshi(), useTrampoline = true)
        ).toSendPaymentResult()

    fun payLnUrl(requestData: LnUrlPayData, amount: Long, comment: String): LnUrlPayOutcome =
        breezSdk.payLnurl(
            LnUrlPayRequest(data = requestData.raw, amountMsat = amount.milliSatoshi(), comment = comment, useTrampoline = true)
        ).toLnUrlPayOutcome()

    fun authLnUrl(requestData: LnUrlAuthData): LnUrlAuthOutcome =
        breezSdk.lnurlAuth(
            LnUrlAuthRequestData(
                k1 = requestData.k1,
                domain = requestData.domain,
                url = requestData.url,
                action = requestData.action,
            )
        ).toLnUrlAuthOutcome()

    fun withdrawLnUrl(requestData: LnUrlWithdrawData, amount: Long, description: String?): LnUrlWithdrawOutcome =
        breezSdk.withdrawLnurl(
            breez_sdk.LnUrlWithdrawRequest(
                data = breez_sdk.LnUrlWithdrawRequestData(
                    callback = requestData.callback,
                    k1 = requestData.k1,
                    defaultDescription = requestData.defaultDescription,
                    minWithdrawable = requestData.minWithdrawable,
                    maxWithdrawable = requestData.maxWithdrawable,
                ),
                amountMsat = amount.milliSatoshi(),
                description = description,
            )
        ).toLnUrlWithdrawOutcome()

    fun generateDiagnosticData(): String? = breezSdk.generateDiagnosticData()

    fun sync() = breezSdk.sync()

    fun registerWebhook(url: String) {
        breezSdk.registerWebhook(url)
    }

    fun disconnect() {
        tryCatchNull<Unit> { breezSdk.disconnect() }
    }

    companion object: Loggable()
}
