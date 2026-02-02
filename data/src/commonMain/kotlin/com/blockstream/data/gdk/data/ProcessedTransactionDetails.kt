package com.blockstream.data.gdk.data

import com.blockstream.data.extensions.isNotBlank
import com.blockstream.data.gdk.GreenJson
import com.blockstream.data.lightning.LnUrlPayOutcome
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProcessedTransactionDetails constructor(
    @SerialName("txhash")
    val txHash: String? = null,
    @SerialName("send_all")
    val isSendAll: Boolean = false,
    @SerialName("transaction")
    val transaction: String? = null,
    @SerialName("signed_transaction")
    val signedTransaction: String? = null,
    @SerialName("psbt")
    val psbt: String? = null,
    @SerialName("payment_id")
    val paymentId: String? = null,
    @SerialName("message")
    val message: String? = null,
    @SerialName("url")
    val url: String? = null,
) : GreenJson<ProcessedTransactionDetails>() {

    val hasMessageOrUrl
        get() = message.isNotBlank() || url.isNotBlank()

    override fun kSerializer() = serializer()

    companion object {
        fun create(success: LnUrlPayOutcome.Success): ProcessedTransactionDetails {
            return ProcessedTransactionDetails(message = success.message, url = success.url)
        }
    }
}
