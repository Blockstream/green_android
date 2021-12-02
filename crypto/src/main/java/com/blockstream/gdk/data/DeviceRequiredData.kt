package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import com.greenaddress.greenapi.data.InputOutputData
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class DeviceRequiredData constructor(
    @SerialName("action") val action: String,
    @SerialName("device") val device: Device,
    @SerialName("paths") val paths: List<List<Int>>? = null,
    @SerialName("path") val path: List<Int>? = null,
    @SerialName("message") val message: String? = null,

    @SerialName("use_ae_protocol") val useAeProtocol: Boolean? = null,
    @SerialName("ae_host_commitment") val aeHostCommitment: String? = null,
    @SerialName("ae_host_entropy") val aeHostEntropy: String? = null,

    @SerialName("signing_inputs") val signingInputs: List<InputOutput>? = null,
    @SerialName("transaction_outputs") val transactionOutputs: List<InputOutput>? = null,
    @SerialName("signing_transactions") val signingTransactions: Map<String, String>? = null,
    @SerialName("signing_address_types") val signingAddressTypes: List<String>? = null,

    @SerialName("scripts") val scripts: List<String>? = null,
    @SerialName("public_keys") val publicKeys: List<String>? = null,

    @SerialName("transaction") val transaction: JsonElement? = null,
): GAJson<DeviceRequiredData>() {

    override fun kSerializer(): KSerializer<DeviceRequiredData> {
        return serializer()
    }

    fun getSigningInputsAsInputOutputData(): List<InputOutputData>{
        return signingInputs?.map {
            it.toInputOutputData()
        } ?: arrayListOf()
    }

    fun getTransactionOutputsAsInputOutputData(): List<InputOutputData>{
        return transactionOutputs?.map {
            it.toInputOutputData()
        } ?: arrayListOf()
    }
}