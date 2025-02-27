package com.blockstream.common.devices

/**
 * A channel to transcieve ISO7816-4 APDUs.
 */
interface CardChannel {
    /**
     * Sends the given C-APDU and returns an R-APDU.
     *
     * @param cmd the command to send
     * @return the card response
     * @throws IOException communication error
     */
    fun send(cmd: ApduCommand): ApduResponse

    /**
     * True if connected, false otherwise
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean
}