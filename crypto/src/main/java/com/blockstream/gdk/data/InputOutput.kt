package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Note: all fields are Optionals with default value null as there is no documentation to guarantee
 * the json structure
 */

@Serializable
data class InputOutput(
    @SerialName("address") val address: String? = null,
    @SerialName("addressee") val addressee: String? = null,
    @SerialName("address_type") val addressType: String? = null,

    // Maybe se it to null ?
    @SerialName("is_change") val isChange: Boolean? = null,
    @SerialName("is_output") val isOutput: Boolean,
    @SerialName("is_relevant") val isRelevant: Boolean,
    @SerialName("is_spent") val isSpent: Boolean,

    @SerialName("pointer") val pointer: Int,
    @SerialName("prevout_script") val prevoutScript: String? = null,
    @SerialName("pt_idx") val ptIdx: Long,
    @SerialName("recovery_xpub") val recoveryXpub: String? = null,

    @SerialName("satoshi") val satoshi: Long? = null,
    @SerialName("script") val script: String? = null,
    @SerialName("script_type") val scriptType: Int,

    @SerialName("sequence") val sequence: Long,
    @SerialName("subaccount") val subaccount: Int,
    @SerialName("subtype") val subtype: Int,

    @SerialName("txhash") val txhash: String,
    @SerialName("service_spub") val serviceXpub: String? = null,

    @SerialName("user_path") val userPath: List<Long>? = null,

    @SerialName("commitment") val commitment: String? = null,
    @SerialName("assetblinder") val assetblinder: String? = null,
    @SerialName("amountblinder") val amountblinder: String? = null,
    @SerialName("public_key") val publicKey: String? = null,
)