package com.blockstream.common.devices

import java.util.logging.Logger
import java.util.logging.Level
import java.io.IOException

/**
 * This class is used to send APDU to the applet. Each method corresponds to an APDU as defined in the APPLICATION.md
 * file. Some APDUs map to multiple methods for the sake of convenience since their payload or response require some
 * pre/post processing.
 */
class SatochipCommandSet(private val apduChannel: CardChannel) {

    companion object {
        private val logger: Logger = Logger.getLogger("org.satochip.client")

        val SATOCHIP_AID: ByteArray = hexToBytes("5361746f43686970") // SatoChip

        /* s must be an even-length string. */
        fun hexToBytes(s: String): ByteArray {
            val len = s.length
            val data = ByteArray(len / 2)
            for (i in 0 until len step 2) {
                data[i / 2] = ((Character.digit(s[i], 16) shl 4) +
                        Character.digit(s[i + 1], 16)).toByte()
            }
            return data
        }
    }

    // Satochip or...
    private var cardType: String? = null

    init {
        logger.level = Level.WARNING
    }

    @Throws(IOException::class)
    fun cardSelect(cardType: String): ApduResponse {
        val selectApplet = when (cardType) {
            "satochip" -> ApduCommand(0x00, 0xA4, 0x04, 0x00, SATOCHIP_AID)
            else -> ApduCommand(0x00, 0xA4, 0x04, 0x00, SATOCHIP_AID)
        }

        logger.info("SATOCHIPLIB: C-APDU cardSelect:${selectApplet.toHexString()}")
        val respApdu = apduChannel.send(selectApplet)
        logger.info("SATOCHIPLIB: R-APDU cardSelect:${respApdu.toHexString()}")

        if (respApdu.getSw() == 0x9000) {
            this.cardType = cardType
            logger.info("SATOCHIPLIB: Satochip-java: CardSelect: found a ${this.cardType}")
        }
        return respApdu
    }

}