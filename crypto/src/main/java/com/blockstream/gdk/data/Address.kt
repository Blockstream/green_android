package com.blockstream.gdk.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Address(
    @SerialName("address") val address: String,
    @SerialName("pointer") val pointer: Int,
    @SerialName("address_type") val addressType: String? = null,
    @SerialName("branch") val branch: Int? = null,
    @SerialName("script") val script: String? = null,
    @SerialName("script_type") val scriptType: Int? = null,
    @SerialName("subaccount") val subaccount: Int? = null,
    @SerialName("subtype") val subType: String? = null,
) : Parcelable