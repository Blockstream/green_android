package com.blockstream.common.data

import com.blockstream.common.gdk.GreenJson
import com.blockstream.common.gdk.data.Settings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WalletExtras(
    @SerialName("settings")
    val settings: Settings? = null,
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