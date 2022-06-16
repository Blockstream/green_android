package com.blockstream.green.di

import android.content.Context
import android.hardware.usb.UsbManager
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.gdk.SessionManager
import com.polidea.rxandroidble2.RxBleClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
class DeviceModule {

    @Singleton
    @Provides
    fun provideDeviceManager(@ApplicationContext context: Context, sessionManager: SessionManager): DeviceManager {
        return DeviceManager(
            context,
            sessionManager,
            context.getSystemService(Context.USB_SERVICE) as UsbManager,
            RxBleClient.create(context)
        )
    }
}
