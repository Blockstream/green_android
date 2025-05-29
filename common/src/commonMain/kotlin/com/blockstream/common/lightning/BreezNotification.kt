package com.blockstream.common.lightning

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BreezNotification(
    @SerialName("payment_hash")
    val paymentHash: String? = null,
    @SerialName("tx_id")
    val txId: String? = null,
) : GreenJson<BreezNotification>() {
    override fun kSerializer() = serializer()

    val isPayment by lazy { paymentHash != null }

    val isTransactionConfirmation by lazy { txId != null }

    companion object {
        fun fromString(jsonString: String?): BreezNotification? = try {
            json.decodeFromString(jsonString ?: "")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
