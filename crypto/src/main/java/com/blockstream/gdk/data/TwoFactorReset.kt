package com.blockstream.gdk.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class TwoFactorReset constructor(
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("days_remaining") val daysRemaining: Int = 0,
    @SerialName("is_disputed") val isDisputed: Boolean? = null
) : Parcelable
