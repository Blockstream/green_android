package com.blockstream.gdk.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Note: all fields are Optionals with default value null as there is no documentation to guarantee
 * the json structure
 */

@Serializable
@Parcelize
data class InputOutput(
    @SerialName("address") val address: String? = null,
    @SerialName("addressee") val addressee: String? = null,
    @SerialName("address_type") val addressType: String? = null,

    // Maybe se it to null ?
    @SerialName("is_change") val isChange: Boolean? = null,
    // @SerialName("is_output") val isOutput: Boolean, // Singlesig inconsistencies in .46
    @SerialName("is_relevant") val isRelevant: Boolean,
    // @SerialName("is_spent") val isSpent: Boolean, // Singlesig inconsistencies in .46

    @SerialName("pointer") val pointer: Int,
    @SerialName("prevout_script") val prevoutScript: String? = null,
    @SerialName("pt_idx") val ptIdx: Long,
    @SerialName("recovery_xpub") val recoveryXpub: String? = null,

    @SerialName("satoshi") val satoshi: Long? = null,
    @SerialName("script") val script: String? = null,
    @SerialName("script_type") val scriptType: Int,

    @SerialName("sequence") val sequence: Long? = null,
    @SerialName("subaccount") val subaccount: Int,
    @SerialName("subtype") val subtype: Int,

    @SerialName("txhash") val txhash: String? = null,
    @SerialName("service_spub") val serviceXpub: String? = null,

    @SerialName("user_path") val userPath: List<Long>? = null,

    @SerialName("commitment") val commitment: String? = null, // blinded value
    @SerialName("assetblinder") val assetblinder: String? = null, // asset blinding factor
    @SerialName("amountblinder") val amountblinder: String? = null, // value blinding factor
    @SerialName("asset_id") val assetId: String? = null, // asset id for Liquid txs
    @SerialName("public_key") val publicKey: String? = null, // the pubkey embedded into the blinded address we are sending to

    @SerialName("eph_keypair_sec") val ephKeypairSec: String? = null, // our secret key used for the blinding
    @SerialName("eph_keypair_pub") val ephKeypairPub: String? = null, // and the public key
) : Parcelable {

    fun getUnblindedString(): String? = if (hasUnblindingData()) {
        // <value_in_satoshi>,<asset_id_hex>,<amount_blinder_hex>,<asset_blinder_hex>
        String.format("%d,%s,%s,%s", satoshi, assetId, amountblinder, assetblinder)
    } else null

    fun hasUnblindingData(): Boolean {
        return assetId != null && satoshi != null && assetblinder != null && amountblinder != null && assetId.isNotEmpty() && amountblinder.isNotEmpty() && assetblinder.isNotEmpty()
    }
}