package com.blockstream.common.data

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.Serializable

@Serializable
data class WalletExtras(
    val totalBalanceInFiat: Boolean = false
) : GreenJson<WalletExtras>() {
    override fun kSerializer() = serializer()

    companion object {
        fun fromString(jsonString: String): WalletExtras {
            return try {
                json.decodeFromString(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                WalletExtras()
            }
        }
    }
}

fun String.toWalletExtras(): WalletExtras = WalletExtras.fromString(this)