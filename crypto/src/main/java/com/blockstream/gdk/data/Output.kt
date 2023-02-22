package com.blockstream.gdk.data

import breez_sdk.LnInvoice
import breez_sdk.LnUrlPayRequestData
import com.blockstream.lightning.amountSatoshi
import com.blockstream.lightning.sendableSatoshi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Output constructor(
    @SerialName("address") val address: String? = null,
    @SerialName("domain") val domain: String? = null,
    @SerialName("asset_id") val assetId: String? = null,
    @SerialName("is_change") val isChange: Boolean,
    @SerialName("satoshi") val satoshi: Long,
){
    companion object{

        fun fromInvoice(invoice: LnInvoice, fallbackAmount: Long): Output {
            return Output(
                address = invoice.bolt11,
                satoshi = (invoice.amountSatoshi() ?: fallbackAmount).let { -it },
                isChange = false
            )
        }
        fun fromLnUrlPay(requestData: LnUrlPayRequestData, input: String, satoshi: Long): Output {
            return Output(
                address = input,
                satoshi = -(requestData.sendableSatoshi(satoshi) ?: 0),
                domain = requestData.domain,
                isChange = false
            )
        }
    }
}