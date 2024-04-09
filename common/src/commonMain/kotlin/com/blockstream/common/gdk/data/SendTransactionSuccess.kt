package com.blockstream.common.gdk.data

import breez_sdk.AesSuccessActionDataResult
import breez_sdk.LnUrlPaySuccessData
import breez_sdk.SuccessActionProcessed
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class SendTransactionSuccess constructor(
    @SerialName("txhash") val txHash: String? = null,
    @SerialName("send_all") val isSendAll: Boolean = false,
    @SerialName("signed_transaction") val signedTransaction: String? = null,
    @SerialName("payment_id") val paymentId: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("url") val url: String? = null,
) : GreenJson<SendTransactionSuccess>(), Parcelable {

    val hasMessageOrUrl
        get() = message.isNotBlank() || url.isNotBlank()

    override fun kSerializer() = serializer()

    companion object {
        fun create(successData: LnUrlPaySuccessData): SendTransactionSuccess{

            val message = successData.successAction?.let {
                when(it){
                    is SuccessActionProcessed.Aes -> {
                        (it.result as? AesSuccessActionDataResult.Decrypted)?.let { decrypted ->
                            "${decrypted.data.description}\n\n${decrypted.data.plaintext}"
                        } ?: ""
                    }
                    is SuccessActionProcessed.Message -> {
                        it.data.message
                    }
                    is SuccessActionProcessed.Url -> {
                        it.data.description
                    }
                }
            }

            val url = (successData.successAction as? SuccessActionProcessed.Url)?.data?.url

            return SendTransactionSuccess(message = message, url = url)
        }
    }
}
