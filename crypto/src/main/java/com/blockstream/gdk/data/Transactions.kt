package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class Transactions(
    @SerialName("transactions") val transactions: List<Transaction>,
)