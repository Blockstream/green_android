package com.blockstream.data.gdk.data

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TwoFactorMethodConfig constructor(
    @SerialName("confirmed")
    val confirmed: Boolean = false,
    @SerialName("enabled")
    val enabled: Boolean = false,
    @SerialName("data")
    val data: String = "",
    @SerialName("is_sms_backup")
    val isSmsBackup: Boolean = false,
) : GreenJson<TwoFactorMethodConfig>() {

    override fun kSerializer() = serializer()
}