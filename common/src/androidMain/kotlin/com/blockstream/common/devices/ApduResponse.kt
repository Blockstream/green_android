package com.blockstream.common.devices

/**
 * ISO7816-4 APDU response.
 */
class ApduResponse {
    private var apdu: ByteArray = byteArrayOf()
    private var data: ByteArray = byteArrayOf()
    private var sw: Int = 0
    private var sw1: Int = 0
    private var sw2: Int = 0

    /**
     * Creates an APDU object by parsing the raw response from the card.
     *
     * @param apdu the raw response from the card.
     */
    constructor(apdu: ByteArray) {
        require(apdu.size >= 2) { "APDU response must be at least 2 bytes" }
        this.apdu = apdu
        parse()
    }

    constructor(data: ByteArray, sw1: Byte, sw2: Byte) {
        val apduArray = ByteArray(data.size + 2)
        System.arraycopy(data, 0, apduArray, 0, data.size)
        apduArray[data.size] = sw1
        apduArray[data.size + 1] = sw2
        this.apdu = apduArray
        parse()
    }

    /**
     * Parses the APDU response, separating the response data from SW.
     */
    private fun parse() {
        val length = apdu.size

        sw1 = apdu[length - 2].toInt() and 0xff
        sw2 = apdu[length - 1].toInt() and 0xff
        sw = (sw1 shl 8) or sw2

        data = ByteArray(length - 2)
        System.arraycopy(apdu, 0, data, 0, length - 2)
    }

    /**
     * Returns true if the SW is 0x9000.
     *
     * @return true if the SW is 0x9000.
     */
    fun isOK(): Boolean = sw == SW_OK

    /**
     * Asserts that the SW is 0x9000. Throws an exception if it isn't
     *
     * @return this object, to simplify chaining
     * @throws ApduException if the SW is not 0x9000
     */
    @Throws(ApduException::class)
    fun checkOK(): ApduResponse = checkSW(SW_OK)

    /**
     * Asserts that the SW is contained in the given list. Throws an exception if it isn't.
     *
     * @param codes the list of SWs to match.
     * @return this object, to simplify chaining
     * @throws ApduException if the SW is not 0x9000
     */
    @Throws(ApduException::class)
    fun checkSW(vararg codes: Int): ApduResponse {
        for (code in codes) {
            if (sw == code) {
                return this
            }
        }

        when (sw) {
            SW_SECURITY_CONDITION_NOT_SATISFIED ->
                throw ApduException(sw, "security condition not satisfied")
            SW_AUTHENTICATION_METHOD_BLOCKED ->
                throw ApduException(sw, "authentication method blocked")
            else ->
                throw ApduException(sw, "Unexpected error SW")
        }
    }

    /**
     * Asserts that the SW is 0x9000. Throws an exception with the given message if it isn't
     *
     * @param message the error message
     * @return this object, to simplify chaining
     * @throws ApduException if the SW is not 0x9000
     */
    @Throws(ApduException::class)
    fun checkOK(message: String): ApduResponse = checkSW(message, SW_OK)

    /**
     * Asserts that the SW is contained in the given list. Throws an exception with the given message if it isn't.
     *
     * @param message the error message
     * @param codes the list of SWs to match.
     * @return this object, to simplify chaining
     * @throws ApduException if the SW is not 0x9000
     */
    @Throws(ApduException::class)
    fun checkSW(message: String, vararg codes: Int): ApduResponse {
        for (code in codes) {
            if (sw == code) {
                return this
            }
        }
        throw ApduException(sw, message)
    }

    /**
     * Serializes the APDU to human readable hex string format
     *
     * @return the hex string representation of the APDU
     */
    fun toHexString(): String {
        return try {
            if (apdu.isEmpty()) {
                ""
            } else {
                StringBuilder(2 * apdu.size).apply {
                    apdu.forEach { b ->
                        append(HEXES[(b.toInt() and 0xF0) shr 4])
                        append(HEXES[b.toInt() and 0x0F])
                    }
                }.toString()
            }
        } catch (e: Exception) {
            "Exception in ApduResponse.toHexString()"
        }
    }

    companion object {
        const val SW_OK = 0x9000
        const val SW_SECURITY_CONDITION_NOT_SATISFIED = 0x6982
        const val SW_AUTHENTICATION_METHOD_BLOCKED = 0x6983
        const val SW_CARD_LOCKED = 0x6283
        const val SW_REFERENCED_DATA_NOT_FOUND = 0x6A88
        const val SW_CONDITIONS_OF_USE_NOT_SATISFIED = 0x6985 // applet may be already installed
        const val SW_WRONG_PIN_MASK = 0x63C0
        const val SW_WRONG_PIN_LEGACY = 0x9C02
        const val SW_BLOCKED_PIN = 0x9C0C
        const val SW_FACTORY_RESET = 0xFF00
        const val HEXES = "0123456789ABCDEF"
    }

    // Getters converted to Kotlin properties
    fun getData(): ByteArray = data
    fun getSw(): Int = sw
    fun getSw1(): Int = sw1
    fun getSw2(): Int = sw2
    fun getBytes(): ByteArray = apdu
}