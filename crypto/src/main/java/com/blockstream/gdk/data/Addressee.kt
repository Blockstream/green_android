package com.blockstream.gdk.data

import breez_sdk.LnInvoice
import breez_sdk.LnUrlPayRequestData
import com.blockstream.gdk.GAJson
import com.blockstream.lightning.amountSatoshi
import com.blockstream.lightning.isAmountLocked
import com.blockstream.lightning.maxSendableSatoshi
import com.blockstream.lightning.metadata
import com.blockstream.lightning.minSendableSatoshi
import com.blockstream.lightning.sendableSatoshi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Addressee constructor(
    @SerialName("address") val address: String,
    @SerialName("asset_id") val assetId: String? = null,
    @SerialName("satoshi") val satoshi: Long? = null,
    @SerialName("bip21-params") val bip21Params: Bip21Params? = null,
    @SerialName("has_locked_amount") val hasLockedAmount: Boolean? = null,
    @SerialName("min_amount") val minAmount: Long? = null,
    @SerialName("max_amount") val maxAmount: Long? = null,
    @SerialName("domain") val domain: String? = null,
    @SerialName("metadata") val metadata: List<List<String>>? = null,
) : GAJson<Addressee>() {
    override fun kSerializer() = serializer()

    companion object{
        fun fromInvoice(invoice: LnInvoice, fallbackAmount: Long): Addressee {
            return Addressee(
                address = invoice.bolt11,
                satoshi = (invoice.amountSatoshi() ?: fallbackAmount).let { -it },
                hasLockedAmount = invoice.amountMsat != null,
            )
        }

        fun fromLnUrlPay(requestData: LnUrlPayRequestData, input: String, satoshi: Long): Addressee {
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
    }
}