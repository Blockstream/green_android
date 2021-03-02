package com.blockstream.gdk.data

import com.blockstream.gdk.serializers.AccountTypeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubAccount(
    @SerialName("name") val name: String,
    @SerialName("pointer") val pointer: Long,
    @SerialName("has_transactions") val hasTransactions: Boolean,
    @SerialName("receiving_id") val receivingId: String,
    @SerialName("recovery_chain_code") val recoveryChainCode: String = "",

    @Serializable(with = AccountTypeSerializer::class)
    @SerialName("type") val type: AccountType,
    @SerialName("satoshi") val satoshi: Map<String, Long>
){
    fun nameOrDefault(default: String): String = name.ifBlank { default }
}