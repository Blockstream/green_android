package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceRequiredData constructor(
    @SerialName("action") val action: String,
    @SerialName("device") val device: Device,
    @SerialName("paths") val paths: List<List<UInt>>? = null,
    @SerialName("path") val path: List<UInt>? = null,
    @SerialName("message") val message: String? = null,

    @SerialName("blinding_keys_required") val blindingKeysRequired: Boolean? = null,
    @SerialName("use_ae_protocol") val useAeProtocol: Boolean? = null,
    @SerialName("ae_host_commitment") val aeHostCommitment: String? = null,
    @SerialName("ae_host_entropy") val aeHostEntropy: String? = null,

    @SerialName("transaction_inputs") val transactionInputs: List<InputOutput>? = null,
    @SerialName("transaction_outputs") val transactionOutputs: List<InputOutput>? = null,
    @SerialName("signing_transactions") val signingTransactions: Map<String, String>? = null,

    @SerialName("scripts") val scripts: List<String>? = null,
    @SerialName("public_keys") val publicKeys: List<String>? = null,

    @SerialName("transaction") val transaction: String? = null,
): GreenJson<DeviceRequiredData>() {

    override fun kSerializer() = serializer()
}
