package com.blockstream.common.devices

import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.ArrayList
import java.util.Arrays
import java.util.logging.Logger
import java.util.logging.Level
import java.io.IOException
import java.io.InputStream
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.cert.CertPathValidator
import java.security.cert.CertPath
import java.security.cert.CertificateFactory
import java.security.cert.Certificate
import java.security.cert.PKIXParameters
import java.security.KeyStore
import java.security.PublicKey

/**
 * This class is used to send APDU to the applet. Each method corresponds to an APDU as defined in the APPLICATION.md
 * file. Some APDUs map to multiple methods for the sake of convenience since their payload or response require some
 * pre/post processing.
 */
class SatochipCommandSet(private val apduChannel: CardChannel) {

    companion object {
        private val logger: Logger = Logger.getLogger("org.satochip.client")
        const val INS_GET_STATUS: Int = 0x3C

        val SATOCHIP_AID: ByteArray = hexToBytes("5361746f43686970") // SatoChip
        val SEEDKEEPER_AID: ByteArray = hexToBytes("536565644b6565706572") // SeedKeeper
        val SATODIME_AID: ByteArray = hexToBytes("5361746f44696d65") // SatoDime

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

    private var status: ApplicationStatus? = null
    private var pin0: ByteArray? = null
    private val possibleAuthentikeys = ArrayList<ByteArray>()
    private var authentikey: ByteArray? = null
    private var authentikeyHex: String? = null
    private var defaultBip32path: String? = null
    private var extendedKey: ByteArray? = null
    private var extendedChaincode: ByteArray? = null
    private var extendedKeyHex: String? = null
    private var extendedPrivKey: ByteArray? = null
    private var extendedPrivKeyHex: String? = null

    // Satodime, SeedKeeper or Satochip?
    private var cardType: String? = null
    private var certPem: String? = null // PEM certificate of device, if any

    init {
        logger.level = Level.WARNING
    }

    fun setLoggerLevel(level: String) {
        logger.level = when (level) {
            "info" -> Level.INFO
            "warning" -> Level.WARNING
            else -> Level.WARNING
        }
    }

    fun setLoggerLevel(level: Level) {
        logger.level = level
    }

    fun getApplicationStatus(): ApplicationStatus? = status

    fun getPossibleAuthentikeys(): List<ByteArray> = possibleAuthentikeys

    fun setDefaultBip32path(bip32path: String) {
        defaultBip32path = bip32path
    }

    fun cardTransmit(plainApdu: ApduCommand): ApduResponse {
        var isApduTransmitted = false
        do {
            try {
                val apduBytes = plainApdu.serialize()
                val ins = apduBytes[1].toUByte().toInt()
                var isEncrypted = false

                // check if status available
                if (status == null) {
                    val statusCapdu = ApduCommand(0xB0, INS_GET_STATUS, 0x00, 0x00, ByteArray(0))
                    val statusRapdu = apduChannel.send(statusCapdu)
                    status = ApplicationStatus(statusRapdu)
                    logger.info("SATOCHIPLIB: Status cardGetStatus:${status.toString()}")
                }

                val capdu = if (status?.needsSecureChannel() == true &&
                    ins != 0xA4 &&
                    ins != 0x81 &&
                    ins != 0x82 &&
                    ins != INS_GET_STATUS) {
                    isEncrypted = true
                    plainApdu
                } else {
                    plainApdu
                }

                val rapdu = apduChannel.send(capdu)
                val sw12 = rapdu.getSw()

                when (sw12) {
                    0x9000 -> {
                        isApduTransmitted = true
                        return rapdu
                    }
                    0x9C21 -> {
                        // SecureChannel is not initialized
                    }
                    else -> {
                        isApduTransmitted = true
                        return rapdu
                    }
                }

            } catch (e: Exception) {
                logger.warning("SATOCHIPLIB: Exception in cardTransmit: $e")
                return ApduResponse(ByteArray(0), 0x00, 0x00)
            }
        } while (!isApduTransmitted)

        return ApduResponse(ByteArray(0), 0x00, 0x00)
    }

    fun cardDisconnect() {
        status = null
        pin0 = null
    }

    @Throws(IOException::class)
    fun cardSelect(): ApduResponse {
        var rapdu = cardSelect("satochip")
        if (rapdu.getSw() != 0x9000) {
            rapdu = cardSelect("seedkeeper")
            if (rapdu.getSw() != 0x9000) {
                rapdu = cardSelect("satodime")
                if (rapdu.getSw() != 0x9000) {
                    cardType = "unknown"
                    logger.warning("SATOCHIPLIB: CardSelect: could not select a known applet")
                }
            }
        }
        return rapdu
    }

    @Throws(IOException::class)
    fun cardSelect(cardType: String): ApduResponse {
        val selectApplet = when (cardType) {
            "satochip" -> ApduCommand(0x00, 0xA4, 0x04, 0x00, SATOCHIP_AID)
            "seedkeeper" -> ApduCommand(0x00, 0xA4, 0x04, 0x00, SEEDKEEPER_AID)
            else -> ApduCommand(0x00, 0xA4, 0x04, 0x00, SATODIME_AID)
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

    fun cardGetStatus(): ApduResponse {
        val plainApdu = ApduCommand(0xB0, INS_GET_STATUS, 0x00, 0x00, ByteArray(0))

        logger.info("SATOCHIPLIB: C-APDU cardGetStatus:${plainApdu.toHexString()}")
        val respApdu = cardTransmit(plainApdu)
        logger.info("SATOCHIPLIB: R-APDU cardGetStatus:${respApdu.toHexString()}")

        status = ApplicationStatus(respApdu)
        logger.info("SATOCHIPLIB: Status from cardGetStatus:${status.toString()}")

        return respApdu
    }
}