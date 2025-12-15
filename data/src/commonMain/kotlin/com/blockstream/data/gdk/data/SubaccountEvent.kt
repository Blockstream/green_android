package com.blockstream.data.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubaccountEvent(
    @SerialName("pointer")
    val pointer: Int = 0,
    @SerialName("event_type")
    val eventType: String = "",
) {
    val isSynced: Boolean
        get() = eventType == "synced"
}