package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import com.blockstream.gdk.serializers.AccountTypeSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.greenaddress.greenapi.data.SubaccountData
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SubAccount(
    @SerialName("name") val name: String,
    @SerialName("pointer") val pointer: Long,
    @SerialName("receiving_id") val receivingId: String,
    @SerialName("recovery_pub_key") val recoveryPubKey: String = "",
    @SerialName("recovery_chain_code") val recoveryChainCode: String = "",
    @Serializable(with = AccountTypeSerializer::class)
    @SerialName("type") val type: AccountType,
) : GAJson<SubAccount>() {

    override fun kSerializer(): KSerializer<SubAccount> {
        return serializer()
    }

    fun nameOrDefault(default: String): String = name.ifBlank { default }

    private val objectMapper by lazy { ObjectMapper() }

    fun getSubaccountDataV3() : SubaccountData {
        return objectMapper.treeToValue(
            objectMapper.readTree(Json.encodeToString(this)),
            SubaccountData::class.java
        )
    }
}