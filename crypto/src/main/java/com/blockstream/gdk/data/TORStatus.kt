package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TORStatus(
    @SerialName("progress") val progress: Int,
    @SerialName("summary") val summary: String,
    @SerialName("tag") val tag: String,
)