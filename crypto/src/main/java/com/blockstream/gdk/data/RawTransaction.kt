package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import com.blockstream.gdk.serializers.DateSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.greenaddress.greenapi.data.TransactionData
import com.greenaddress.greenapi.data.TwoFactorStatusData
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.util.*

@Serializable
data class RawTransaction(
    @SerialName("addressees") val addressees: List<String>
) : GAJson<RawTransaction>() {
    override fun kSerializer(): KSerializer<RawTransaction> = serializer()
}