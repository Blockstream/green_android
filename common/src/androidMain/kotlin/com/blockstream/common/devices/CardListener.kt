package com.blockstream.common.devices

/**
 * Listener for card connection events.
 */
interface CardListener {
    /**
     * Executes when the card channel is connected.
     *
     * @param channel the connected card channel
     */
    fun onConnected(channel: CardChannel)

    /**
     * Executes when a previously connected card is disconnected.
     */
    fun onDisconnected()
}