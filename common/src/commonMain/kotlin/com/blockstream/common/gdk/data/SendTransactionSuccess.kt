package com.blockstream.common.gdk.data

import breez_sdk.Payment
import breez_sdk.SuccessActionProcessed
import com.blockstream.common.gdk.GreenJson
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
    val payment: Payment? = null,
    @Transient
    val successAction: SuccessActionProcessed? = null
) : GreenJson<SendTransactionSuccess>() {

    override fun kSerializer(): KSerializer<SendTransactionSuccess> = serializer()
}
