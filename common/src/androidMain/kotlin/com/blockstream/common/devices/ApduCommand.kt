package com.blockstream.common.devices

import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * ISO7816-4 APDU.
 */
class ApduCommand {
    public val cla: Int
    public val ins: Int
    public val p1: Int
    public val p2: Int
    public val data: ByteArray
    public val needsLE: Boolean

    /**
     * Constructs an APDU with no response data length field. The data field cannot be null, but can be a zero-length array.
     *
     * @param cla class byte
     * @param ins instruction code
     * @param p1 P1 parameter
     * @param p2 P2 parameter
     * @param data the APDU data
     */
    constructor(cla: Int, ins: Int, p1: Int, p2: Int, data: ByteArray) : this(cla, ins, p1, p2, data, false)

    /**
     * Constructs an APDU with an optional data length field. The data field cannot be null, but can be a zero-length array.
     * The LE byte, if sent, is set to 0.
     *
     * @param cla class byte
     * @param ins instruction code
     * @param p1 P1 parameter
     * @param p2 P2 parameter
     * @param data the APDU data
     * @param needsLE whether the LE byte should be sent or not
     */
    constructor(cla: Int, ins: Int, p1: Int, p2: Int, data: ByteArray, needsLE: Boolean) {
        this.cla = cla and 0xff
        this.ins = ins and 0xff
        this.p1 = p1 and 0xff
        this.p2 = p2 and 0xff
        this.data = data
        this.needsLE = needsLE
    }

    /**
     * Serializes the APDU in order to send it to the card.
     *
     * @return the byte array representation of the APDU
     */
    @Throws(IOException::class)
    fun serialize(): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(cla)
        out.write(ins)
        out.write(p1)
        out.write(p2)
        out.write(data.size)
        out.write(data)

        if (needsLE) {
            out.write(0) // Response length
        }

        return out.toByteArray()
    }

    /**
     * Serializes the APDU to human readable hex string format
     *
     * @return the hex string representation of the APDU
     */
    fun toHexString(): String {
        return try {
            val raw = serialize()
            StringBuilder(2 * raw.size).apply {
                raw.forEach { b ->
                    append(HEXES[(b.toInt() and 0xF0) shr 4])
                    append(HEXES[b.toInt() and 0x0F])
                }
            }.toString()
        } catch (e: Exception) {
            "Exception in ApduCommand.toHexString()"
        }
    }

    companion object {
        const val HEXES = "0123456789ABCDEF"
    }
}