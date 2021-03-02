package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkEvent(
    @SerialName("connected") val connected: Boolean,
    @SerialName("heartbeat_timeout") val heartbeatTimeout: Boolean? = null,
    @SerialName("login_required") val loginRequired: Boolean? = null,

    @SerialName("elapsed") val elapsed: Int? = null,
    @SerialName("limit") val limit: Boolean? = null,
    @SerialName("waiting") val waiting: Long? = null
)