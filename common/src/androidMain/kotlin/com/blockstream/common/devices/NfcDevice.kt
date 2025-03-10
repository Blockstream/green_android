package com.blockstream.common.devices


enum class NfcDeviceType {
    SATOCHIP
}

class NfcDevice(val type: NfcDeviceType) {

    var isSeeded = true
    var supportsLiquid = true

}