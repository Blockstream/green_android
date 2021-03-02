package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class SubAccounts(
    @SerialName("subaccounts") val subaccounts: List<SubAccount>,
)