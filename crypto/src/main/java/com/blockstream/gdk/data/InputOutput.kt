package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InputOutput(
    @SerialName("address") val address: String,
    @SerialName("address_type") val addressType: String,
    @SerialName("addressee") val addressee: String,

    @SerialName("is_output") val isOutput: Boolean,
    @SerialName("is_relevant") val isRelevant: Boolean,
    @SerialName("is_spent") val isSpent: Boolean,

    @SerialName("pointer") val pointer: Int,
    @SerialName("pt_idx") val ptIdx: Long,

    @SerialName("satoshi") val satoshi: Long? = null,
    @SerialName("script_type") val scriptType: Int,

    @SerialName("subaccount") val subaccount: Int,
    @SerialName("subtype") val subtype: Int,


    // Add Liquid

)