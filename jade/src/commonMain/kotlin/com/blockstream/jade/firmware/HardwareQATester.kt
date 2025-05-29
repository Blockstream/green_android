package com.blockstream.jade.firmware;

interface HardwareQATester {
    fun getAntiExfilCorruptionForMessageSign(): Boolean
    fun getAntiExfilCorruptionForTxSign(): Boolean
    fun getFirmwareCorruption(): Boolean
}
