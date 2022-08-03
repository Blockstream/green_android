package com.blockstream.gdk.params

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class TransactionParams(
    @SerialName("subaccount") val subaccount: Long,
    @SerialName("first") val offset: Int = 0,
    @SerialName("count") val limit: Int = TRANSACTIONS_PER_PAGE,
    @SerialName("num_confs") val confirmations: Int = 0,
) : GAJson<TransactionParams>() {

    override fun kSerializer(): KSerializer<TransactionParams> {
        return serializer()
    }

    companion object{
        const val TRANSACTIONS_PER_PAGE: Int = 30
    }
}
