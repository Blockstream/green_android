package com.blockstream.common.managers

import com.blockstream.common.di.ApplicationScope
import com.blockstream.common.gdk.device.DeviceInterface
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesIgnore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

open class DeviceManager(scope: ApplicationScope) {
    protected val _usbDevices = MutableStateFlow<List<DeviceInterface>>(listOf())
    protected val _bleDevices = MutableStateFlow<List<DeviceInterface>>(listOf())


    @NativeCoroutinesIgnore
    val devices = combine(_usbDevices, _bleDevices) { ble, usb ->
        ble + usb
    }.stateIn(scope, SharingStarted.WhileSubscribed(), listOf())

    fun getDevice(deviceId: String?): DeviceInterface? {
        return devices.value.find { it.connectionIdentifier == deviceId }
    }
}