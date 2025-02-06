package com.blockstream.common.devices

/**
 * Parses the result of a GET STATUS command retrieving application status.
 */
class ApplicationStatus(rapdu: ApduResponse) {
    private var setupDone: Boolean = false
    private var isSeeded: Boolean = false
    private var needsSecureChannel: Boolean = false
    private var needs2FA: Boolean = false

    private var protocolMajorVersion: Byte = 0
    private var protocolMinorVersion: Byte = 0
    private var appletMajorVersion: Byte = 0
    private var appletMinorVersion: Byte = 0

    private var pin0RemainingTries: Byte = 0
    private var puk0RemainingTries: Byte = 0
    private var pin1RemainingTries: Byte = 0
    private var puk1RemainingTries: Byte = 0

    private var protocolVersion: Int = 0

    init {
        val sw = rapdu.getSw()

        when (sw) {
            0x9000 -> {
                val data = rapdu.getData()
                protocolMajorVersion = data[0]
                protocolMinorVersion = data[1]
                appletMajorVersion = data[2]
                appletMinorVersion = data[3]
                protocolVersion = (protocolMajorVersion.toInt() shl 8) + protocolMinorVersion

                if (data.size >= 8) {
                    pin0RemainingTries = data[4]
                    puk0RemainingTries = data[5]
                    pin1RemainingTries = data[6]
                    puk1RemainingTries = data[7]
                    needs2FA = false // default value
                }
                if (data.size >= 9) {
                    needs2FA = data[8] != 0x00.toByte()
                }
                if (data.size >= 10) {
                    isSeeded = data[9] != 0x00.toByte()
                }
                if (data.size >= 11) {
                    setupDone = data[10] != 0x00.toByte()
                } else {
                    setupDone = true
                }
                if (data.size >= 12) {
                    needsSecureChannel = data[11] != 0x00.toByte()
                } else {
                    needsSecureChannel = false
                    needs2FA = false // default value
                }
            }
            0x9c04 -> {
                setupDone = false
                isSeeded = false
                needsSecureChannel = false
            }
            else -> {
                // throws IllegalArgumentException("Wrong getStatus data!") // should not happen
            }
        }
    }

    // Getters
    fun isSeeded(): Boolean = isSeeded
    fun isSetupDone(): Boolean = setupDone
    fun needsSecureChannel(): Boolean = needsSecureChannel
    fun getPin0RemainingCounter(): Byte = pin0RemainingTries
    fun getPuk0RemainingCounter(): Byte = puk0RemainingTries

    override fun toString(): String {
        return """
            setup_done: $setupDone
            is_seeded: $isSeeded
            needs_2FA: $needs2FA
            needs_secure_channel: $needsSecureChannel
            protocol_major_version: $protocolMajorVersion
            protocol_minor_version: $protocolMinorVersion
            applet_major_version: $appletMajorVersion
            applet_minor_version: $appletMinorVersion
        """.trimIndent()
    }

    fun getCardVersionInt(): Int {
        return (protocolMajorVersion.toInt() * (1 shl 24)) +
                (protocolMinorVersion.toInt() * (1 shl 16)) +
                (appletMajorVersion.toInt() * (1 shl 8)) +
                appletMinorVersion.toInt()
    }

    fun getCardVersionString(): String {
        return "$protocolMajorVersion.$protocolMinorVersion-$appletMajorVersion.$appletMinorVersion"
    }

    fun getProtocolVersion(): Int = protocolVersion
}