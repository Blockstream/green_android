package com.blockstream.common.gdk.data

import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class TwoFactorMethodConfig constructor(
    @SerialName("confirmed") val confirmed: Boolean = false,
    @SerialName("enabled") val enabled: Boolean = false,
    @SerialName("data") val data: String = "",
    @SerialName("is_sms_backup") val isSmsBackup: Boolean = false,
): GreenJson<TwoFactorMethodConfig>(), Parcelable{

    override fun kSerializer() = serializer()
}