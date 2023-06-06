package com.blockstream.common.gdk.params

import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class TransactionParams(
    @SerialName("subaccount") val subaccount: Long,
    @SerialName("first") val offset: Int = 0,
    @SerialName("count") val limit: Int = TRANSACTIONS_PER_PAGE,
    @SerialName("num_confs") val confirmations: Int = 0,
) : GdkJson<TransactionParams>() {

    override fun kSerializer() = serializer()

    companion object{
        const val TRANSACTIONS_PER_PAGE: Int = 30
    }
}
