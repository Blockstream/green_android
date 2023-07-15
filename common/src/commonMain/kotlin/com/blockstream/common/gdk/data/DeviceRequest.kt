package com.blockstream.common.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class DeviceRequest(
    @SerialName("action") val action: String,
    @SerialName("device") val device: Device,
    @SerialName("paths") val paths: List<List<Int>>,
    @SerialName("path") val path: List<Int>,
    @SerialName("message") val message: String,
    @SerialName("transaction") val transaction: JsonElement,

    // TODO InputOutputData
    @SerialName("transaction_inputs") val transactionInputs: List<JsonElement>,
    @SerialName("transaction_outputs") val transactionOutputs: List<JsonElement>,

    @SerialName("signing_transactions") var signingTransactions: Map<String, String>,
    @SerialName("address") var address: Map<String, String>,

    // TODO BlindedScriptsData
    @SerialName("blinded_scripts") var blindedScripts: List<JsonElement>,
)
