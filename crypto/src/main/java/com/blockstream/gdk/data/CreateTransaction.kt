package com.blockstream.gdk.data

import com.blockstream.gdk.GAJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class CreateTransaction constructor(
    @SerialName("addressees") val addressees: List<Addressee> = listOf(),
    @SerialName("satoshi") val satoshi: Map<String, Long> = mapOf(),
    @SerialName("fee") val fee: Long? = null,
    @SerialName("fee_rate") val feeRate: Long? = null,
    @SerialName("addressees_read_only") val isReadOnly: Boolean = false,
    @SerialName("send_all") val isSendAll: Boolean = false,
    @SerialName("transaction_outputs") val outputs: List<Output> = listOf(),
    @SerialName("is_sweep") val isSweep: Boolean = false,
    @SerialName("memo") val memo: String? = null,
    @SerialName("transaction") val transaction: String? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("txhash") val txHash: String? = null,
    @SerialName("sign_with") var signWith: List<String> = listOf(),
) : GAJson<CreateTransaction>() {
    override val keepJsonElement = true

    fun utxoViews(network: Network, showChangeOutputs: Boolean): List<UtxoView> {
        val outputs = outputs.filter {
            !it.address.isNullOrBlank()
        }

        return if(network.isMultisig || (network.isSinglesig && network.isBitcoin)) {
            outputs.filter {
                if (showChangeOutputs || isSweep) {
                    true
                } else {
                    !it.isChange
                }
            }.map {
                UtxoView.fromOutput(it)
            }
        }else{
            // Temp until create_tx singlesig/electrum issues will be addressed by !980
            addressees.map {
                UtxoView(
                    address = it.address,
                    assetId = it.assetId,
                    satoshi = it.satoshi,
                    isChange = false,
                )
            }
        }
    }

    override fun kSerializer(): KSerializer<CreateTransaction> = serializer()
}
