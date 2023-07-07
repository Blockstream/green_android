package com.blockstream.common.gdk.data

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.gdk.GdkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Address constructor(
    @SerialName("address") val address: String,
    @SerialName("pointer") val pointer: Long = 0,
    @SerialName("address_type") val addressType: String? = null,
    @SerialName("branch") val branch: Long = 0,
    @SerialName("tx_count") val txCount: Long? = null,
    @SerialName("script") val script: String? = null,
    @SerialName("script_type") val scriptType: Int? = null,
    @SerialName("subaccount") val subaccount: Int? = null,
    @SerialName("subtype") val subType: Long? = null,
    @SerialName("user_path") val userPath: List<Long>? = null,

    // Used only as AddressParams Sweep
    @SerialName("satoshi") var satoshi: Long = 0,
    @SerialName("is_greedy") var isGreedy: Boolean = true,
) : GdkJson<Address>(), Parcelable {
    override fun encodeDefaultsValues() = true

    override fun kSerializer() = serializer()

    fun userPathAsInt(): List<Int> {
        return userPath?.map { it.toInt() } ?: listOf()
    }
}
