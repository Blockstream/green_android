package com.blockstream;

interface HardwareQATester{
    fun getAntiExfilCorruptionForMessageSign() : Boolean
    fun getAntiExfilCorruptionForTxSign() : Boolean
    fun getFirmwareCorruption() : Boolean
}
