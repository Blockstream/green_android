package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class DeviceRequiredData constructor(
    @SerialName("action") val action: String,
    @SerialName("device") val device: Device,
    @SerialName("paths") val paths: List<List<Long>>? = null,
    @SerialName("path") val path: List<Long>? = null,
    @SerialName("message") val message: String? = null,

     // TODO
    @SerialName("transaction") val transaction: JsonElement? = null,

    @SerialName("signing_address_types") val signingAddressTypes: List<String>? = null,
    @SerialName("signing_inputs") val signingInputs: List<InputOutput>? = null,
    @SerialName("transaction_outputs") val transactionOutputs: List<InputOutput>? = null,
    @SerialName("signing_transactions") val signingTransactions: Map<String, String>? = null,
    @SerialName("address") val address: Map<String, String>? = null,
    @SerialName("blinded_scripts") val blindedScripts: List<BlindedScripts>? = null,
): GAJson<DeviceRequiredData>() {

    override fun kSerializer(): KSerializer<DeviceRequiredData> {
        return DeviceRequiredData.serializer()
    }
}