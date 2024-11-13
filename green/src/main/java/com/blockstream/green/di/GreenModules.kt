@file:OptIn(ExperimentalEncodingApi::class, ExperimentalEncodingApi::class)

package com.blockstream.green.di

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.benasher44.uuid.Uuid
import com.blockstream.common.devices.DeviceManagerAndroid
import com.blockstream.common.fcm.FcmCommon
import com.blockstream.common.interfaces.DeviceConnectionInterface
import com.blockstream.common.managers.DeviceManager
import com.blockstream.compose.devices.LedgerDevice
import com.blockstream.compose.devices.TrezorDevice
import com.blockstream.compose.managers.DeviceConnectionManager
import com.blockstream.compose.managers.DeviceConnectionManagerAndroid
import com.blockstream.green.managers.FcmAndroid
import com.blockstream.green.managers.NotificationManager
import com.blockstream.jade.connection.JadeBleConnection
import com.btchip.comm.LedgerDeviceBLE
import com.juul.kable.Peripheral
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.binds
import org.koin.dsl.module
import kotlin.io.encoding.ExperimentalEncodingApi

val greenModules = module {
    single {
        NotificationManager(
            androidContext(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    single {
        DeviceManagerAndroid(
            get(),
            androidContext(),
            get(),
            get(),
            get(),
            listOf(LedgerDeviceBLE.SERVICE_UUID.toString(), JadeBleConnection.JADE_SERVICE)
        ) { deviceManagerAndroid: DeviceManagerAndroid, usbDevice: UsbDevice?, bleService: Uuid?, peripheral: Peripheral?, isBonded: Boolean? ->
            usbDevice?.let {
                TrezorDevice.fromUsbDevice(deviceManager = deviceManagerAndroid, usbDevice = usbDevice)
                    ?: LedgerDevice.fromUsbDevice(
                        deviceManager = deviceManagerAndroid,
                        usbDevice = usbDevice
                    )
            } ?: peripheral?.let {
                LedgerDevice.fromScan(deviceManager = deviceManagerAndroid, bleService = bleService, peripheral = peripheral, isBonded = isBonded == true)
            }
        }
    } binds (arrayOf(DeviceManager::class, DeviceManagerAndroid::class))
    single {
        DeviceConnectionManagerAndroid(
            get(), get(),get(), get(),get()
        )
    } binds (arrayOf(DeviceConnectionManagerAndroid::class, DeviceConnectionManager::class, DeviceConnectionInterface::class))
    single {
         FcmAndroid(androidContext(), get())
    } binds(arrayOf(FcmCommon::class))
    single {
        androidContext().getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    }
    single {
        androidContext().getSystemService(Context.USB_SERVICE) as UsbManager
    }
}