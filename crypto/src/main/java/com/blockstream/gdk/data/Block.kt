package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Block(
    @SerialName("block_hash") val hash: String? = null,
    @SerialName("block_height") val height: Long,
    @SerialName("initial_timestamp") val timestamp: Long = 0
)