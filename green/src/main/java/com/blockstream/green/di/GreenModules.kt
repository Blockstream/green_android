@file:OptIn(ExperimentalEncodingApi::class, ExperimentalEncodingApi::class)

package com.blockstream.green.di

import android.content.Context
import android.hardware.usb.UsbManager
import com.blockstream.common.fcm.FcmCommon
import com.blockstream.common.managers.DeviceManager
import com.blockstream.green.devices.DeviceManagerAndroid
import com.blockstream.green.managers.FcmAndroid
import com.blockstream.green.managers.NotificationManager
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
            get()
        )
    } binds (arrayOf(DeviceManager::class, DeviceManagerAndroid::class))
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