package com.blockstream.common.devices

/**
 * Exception thrown when the response APDU from the card contains unexpected SW or data.
 */
class ApduException : Exception {
    val sw: Int

    /**
     * Creates an exception with SW and message.
     *
     * @param sw the status word
     * @param message a descriptive message of the error
     */
    constructor(sw: Int, message: String) : super("$message, 0x${String.format("%04X", sw)}") {
        this.sw = sw
    }

    /**
     * Creates an exception with a message.
     *
     * @param message a descriptive message of the error
     */
    constructor(message: String) : super(message) {
        this.sw = 0
    }
}