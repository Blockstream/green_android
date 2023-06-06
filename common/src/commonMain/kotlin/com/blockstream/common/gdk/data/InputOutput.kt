package com.blockstream.common.gdk.data


import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.utils.hexToByteArray
import com.blockstream.common.utils.hexToByteArrayReversed
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Note: all fields are Optionals with default value null as there is no documentation to guarantee
 * the json structure
 */

@Serializable
@Parcelize
data class InputOutput constructor(
    @SerialName("address") val address: String? = null,
    @SerialName("addressee") val addressee: String? = null,
    @SerialName("address_type") val addressType: String? = null,
    @SerialName("is_blinded") val isBlinded: Boolean? = null,
    @SerialName("is_confidential") val isConfidential: Boolean? = null,
    @SerialName("unblinded_address") val unblindedAddress: String? = null,
    @SerialName("is_change") val isChange: Boolean? = null,
    @SerialName("is_output") val isOutput: Boolean? = null,
    @SerialName("is_relevant") val isRelevant: Boolean? = null,
    @SerialName("is_spent") val isSpent: Boolean? = null,

    @SerialName("pointer") val pointer: Int? = null,
    @SerialName("prevout_script") val prevoutScript: String? = null,
    @SerialName("pt_idx") val ptIdx: Long? = null, // this is UInt until Parcelize is supported
    @SerialName("recovery_xpub") val recoveryXpub: String? = null,

    @SerialName("satoshi") val satoshi: Long? = null,
    @SerialName("script") val script: String? = null,
    @SerialName("script_type") val scriptType: Int? = null,
    @SerialName("scriptpubkey") val scriptPubkey: String? = null,

    @SerialName("sequence") val sequence: Long? = null, // this is UInt until Parcelize is supported
    @SerialName("subaccount") val subaccount: Int? = null,
    @SerialName("subtype") val subtype: Int? = null,

    @SerialName("txhash") val txHash: String? = null,
    @SerialName("service_xpub") val serviceXpub: String? = null,

    @SerialName("user_path") val userPath: List<Long>? = null,

    @SerialName("ae_host_commitment") val aeHostCommitment: String? = null,
    @SerialName("ae_host_entropy") val aeHostEntropy: String? = null,

    @SerialName("commitment") val commitment: String? = null, // blinded value
    @SerialName("assetblinder") val assetblinder: String? = null, // asset blinding factor
    @SerialName("amountblinder") val amountblinder: String? = null, // value blinding factor
    @SerialName("asset_id") val assetId: String? = null, // asset id for Liquid txs
    @SerialName("blinding_key") val blindingKey: String? = null, // the blinding public key embedded into the blinded address we are sending to

    @SerialName("eph_public_key") val ephPublicKey: String? = null, // our ephemeral public key for [un]blinding
) : Parcelable {

    // Called from Java to use the UInt.toInt() inline fun
    fun getPtIdxInt() = ptIdx?.toInt() ?: 0
    fun getSequenceInt() = sequence?.toInt() ?: 0

    fun getUnblindedString(): String? = if (hasUnblindingData()) {
        // <value_in_satoshi>,<asset_id_hex>,<amount_blinder_hex>,<asset_blinder_hex>
        "$satoshi,$assetId,$amountblinder,$assetblinder"
    } else null
  
    fun hasUnblindingData(): Boolean {
        return assetId != null && satoshi != null && assetblinder != null && amountblinder != null && assetId.isNotEmpty() && amountblinder.isNotEmpty() && assetblinder.isNotEmpty()
    }

    // FIXME: May soon be available as a json attribute - until then ...
    fun isSegwit(): Boolean {
        return when (addressType) {
            "csv", "p2wsh", "p2wpkh", "p2sh-p2wpkh" -> true
            else -> false
        }
    }

    fun getAssetIdBytes(): ByteArray {
        return assetId.hexToByteArray()
    }

    fun getAbfs(): ByteArray {
        return assetblinder.hexToByteArrayReversed()
    }

    fun getVbfs(): ByteArray {
        return amountblinder.hexToByteArrayReversed()
    }

    fun getTxid(): ByteArray {
        return txHash.hexToByteArrayReversed()
    }

    fun getEphKeypairPubBytes(): ByteArray {
        return ephPublicKey.hexToByteArray()
    }

    fun getPublicKeyBytes(): ByteArray {
        return blindingKey.hexToByteArray()
    }

    fun getRevertedAssetIdBytes(): ByteArray {
        return assetId.hexToByteArrayReversed()
    }

    fun getCommitmentBytes(): ByteArray {
        return commitment.hexToByteArray()
    }

    fun getUserPathAsInts(): List<Int>? {
        return userPath?.map { it.toInt() }
    }
}
