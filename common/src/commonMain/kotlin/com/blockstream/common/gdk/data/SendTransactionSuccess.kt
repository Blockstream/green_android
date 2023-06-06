package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class SendTransactionSuccess(
    @SerialName("txhash") val txHash: String? = null,
    @SerialName("send_all") val isSendAll: Boolean = false,
    @SerialName("signed_transaction") val signedTransaction: String? = null,
    @Transient
    val payment: Any? = null, // Payment
    @Transient
    val successAction: Any? = null // SuccessActionProcessed
) : GdkJson<SendTransactionSuccess>() {

    override fun kSerializer(): KSerializer<SendTransactionSuccess> = serializer()
}
