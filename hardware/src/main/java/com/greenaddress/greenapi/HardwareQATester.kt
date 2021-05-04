package com.greenaddress.greenapi;

interface HardwareQATester{
    fun getAntiExfilCorruptionForMessageSign() : Boolean
    fun getAntiExfilCorruptionForTxSign() : Boolean
}
