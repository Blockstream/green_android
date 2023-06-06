package com.blockstream.common.gdk.data

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class TwoFactorReset constructor(
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("days_remaining") val daysRemaining: Int = 0,
    @SerialName("is_disputed") val isDisputed: Boolean? = null
) : Parcelable
