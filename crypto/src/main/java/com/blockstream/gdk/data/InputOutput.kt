package com.blockstream.gdk.data

import android.os.Parcelable
import com.blockstream.gdk.reverseBytes
import com.blockstream.libwally.Wally
import kotlinx.parcelize.Parcelize
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

    @SerialName("eph_keypair_sec") val ephKeypairSec: String? = null, // our secret key used for the blinding
    @SerialName("eph_keypair_pub") val ephKeypairPub: String? = null, // and the public key
) : Parcelable {

    // Called from Java to use the UInt.toInt() inline fun
    fun getPtIdxInt() = ptIdx?.toInt() ?: 0
    fun getSequenceInt() = sequence?.toInt() ?: 0

    fun getUnblindedString(): String? = if (hasUnblindingData()) {
        // <value_in_satoshi>,<asset_id_hex>,<amount_blinder_hex>,<asset_blinder_hex>
        String.format("%d,%s,%s,%s", satoshi, assetId, amountblinder, assetblinder)
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
        return Wally.hex_to_bytes(assetId)
    }

    fun getAbfs(): ByteArray {
        return Wally.hex_to_bytes(assetblinder).reverseBytes()
    }

    fun getVbfs(): ByteArray {
        return Wally.hex_to_bytes(amountblinder).reverseBytes()
    }

    fun getTxid(): ByteArray {
        return Wally.hex_to_bytes(txHash).reverseBytes()
    }

    fun getEphKeypairPubBytes(): ByteArray {
        return Wally.hex_to_bytes(ephKeypairPub)
    }

    fun getPublicKeyBytes(): ByteArray {
        return Wally.hex_to_bytes(blindingKey)
    }

    fun getRevertedAssetIdBytes(): ByteArray {
        return Wally.hex_to_bytes(assetId).reverseBytes()
    }

    fun getCommitmentBytes(): ByteArray {
        return Wally.hex_to_bytes(commitment)
    }

    fun getUserPathAsInts(): List<Int>? {
        return userPath?.map { it.toInt() }
    }
}
