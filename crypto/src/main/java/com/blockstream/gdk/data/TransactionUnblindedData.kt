package com.blockstream.gdk.data

import android.os.Parcelable
import com.blockstream.gdk.BalancePair
import com.blockstream.gdk.GAJson
import com.blockstream.gdk.serializers.DateSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.greenaddress.greenapi.data.TransactionData
import com.greenaddress.greenapi.data.TwoFactorStatusData
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@Serializable
data class TransactionUnblindedData(
    @SerialName("txid") val txid: String,
    @SerialName("type") val type: String,
    @SerialName("version") val version: Int,
    @SerialName("inputs") val inputs: List<InputUnblindedData>,
    @SerialName("outputs") val outputs: List<OutputUnblindedData>
) : GAJson<TransactionUnblindedData>() {
    override fun kSerializer(): KSerializer<TransactionUnblindedData> = serializer()
}