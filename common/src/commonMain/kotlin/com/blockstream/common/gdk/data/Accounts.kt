package com.blockstream.common.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class Accounts(
    @SerialName("subaccounts") val accounts: List<Account>,
)