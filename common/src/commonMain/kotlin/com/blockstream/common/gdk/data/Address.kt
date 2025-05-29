package com.blockstream.common.gdk.data

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Address constructor(
    @SerialName("address")
    val address: String,
    @SerialName("pointer")
    val pointer: Long = 0,
    @SerialName("address_type")
    val addressType: String? = null,
    @SerialName("branch")
    val branch: Long = 0,
    @SerialName("tx_count")
    val txCount: Long? = null,
    @SerialName("script")
    val script: String? = null,
    @SerialName("subaccount")
    val subaccount: Int? = null,
    @SerialName("subtype")
    val subType: Long? = null,
    @SerialName("user_path")
    val userPath: List<Long>? = null,

    // Used only as AddressParams Sweep
    @SerialName("satoshi")
    var satoshi: Long = 0,
    @SerialName("is_greedy")
    var isGreedy: Boolean = true,
) : GreenJson<Address>() {
    override fun encodeDefaultsValues() = true

    override fun kSerializer() = serializer()

    fun userPathAsInt(): List<Int> {
        return userPath?.map { it.toInt() } ?: listOf()
    }
}
