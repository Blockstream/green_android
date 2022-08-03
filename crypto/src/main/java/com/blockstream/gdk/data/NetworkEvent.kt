package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkEvent constructor(
    @SerialName("current_state") val currentState: String,
    @SerialName("next_state") val nextState: String? = null,
    @SerialName("wait_ms") val wait: Long = 0,
){

    val isConnected
        get() = currentState == KEY_CONNECTED

    val waitInSeconds
        get() = wait / 1000

    companion object{
        private const val KEY_CONNECTED = "connected"
        private const val KEY_DISCONNECTED = "disconnected"

        val ConnectedEvent = NetworkEvent(currentState = KEY_CONNECTED, wait = 0)
        val DisconnectedEvent = NetworkEvent(currentState = KEY_DISCONNECTED, wait = 10000)
    }
}