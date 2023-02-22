package com.blockstream.gdk.data

import android.os.Parcelable
import breez_sdk.LnInvoice
import breez_sdk.SwapInfo
import kotlinx.parcelize.Parcelize
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
) : Parcelable {

    fun getUserPathAsInts(): List<Int>? {
        return userPath?.map { it.toInt() }
    }

    companion object{
        fun fromInvoice(invoice: LnInvoice): Address {
            return Address(address = invoice.bolt11)
        }

        fun fromSwapInfo(swapInfo: SwapInfo): Address {
            return Address(address = swapInfo.bitcoinAddress)
        }
    }
}
