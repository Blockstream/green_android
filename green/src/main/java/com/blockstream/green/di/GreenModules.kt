@file:OptIn(ExperimentalEncodingApi::class, ExperimentalEncodingApi::class)

package com.blockstream.green.di

import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.usb.UsbManager
import com.blockstream.green.BuildConfig
import com.blockstream.green.GreenApplication
import com.blockstream.green.R
import com.blockstream.green.devices.DeviceManager
import com.blockstream.green.managers.NotificationManager
import com.blockstream.green.utils.isDebug
import com.blockstream.green.utils.isDevelopmentOrDebug
import com.pandulapeter.beagle.Beagle
import com.pandulapeter.beagle.common.configuration.Behavior
import com.pandulapeter.beagle.logCrash.BeagleCrashLogger
import com.pandulapeter.beagle.modules.AnimationDurationSwitchModule
import com.pandulapeter.beagle.modules.AppInfoButtonModule
import com.pandulapeter.beagle.modules.BugReportButtonModule
import com.pandulapeter.beagle.modules.DeveloperOptionsButtonModule
import com.pandulapeter.beagle.modules.DeviceInfoModule
import com.pandulapeter.beagle.modules.DividerModule
import com.pandulapeter.beagle.modules.HeaderModule
import com.pandulapeter.beagle.modules.KeylineOverlaySwitchModule
import com.pandulapeter.beagle.modules.LogListModule
import com.pandulapeter.beagle.modules.PaddingModule
import com.pandulapeter.beagle.modules.ScreenCaptureToolboxModule
import com.pandulapeter.beagle.modules.TextModule
import com.polidea.rxandroidble3.RxBleClient
import org.koin.android.ext.koin.androidContext
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
            get(),
            get()
        )
    }
    single {
        DeviceManager(
            androidContext(),
            get(),
            get(),
            get()
        )
    }
    single {
        androidContext().getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    }
    single {
        RxBleClient.create(androidContext())
    }
    single {
        androidContext().getSystemService(Context.USB_SERVICE) as UsbManager
    }
    single {
        (androidContext().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }
    single {
        if (isDevelopmentOrDebug) {
            Beagle.initialize(
                application = androidContext() as GreenApplication,
                behavior = Behavior(
                    bugReportingBehavior = Behavior.BugReportingBehavior(
                        // Enabling this feature will disable the crash collection of Firebase Crashlytics,
                        // as using the two simultaneously has proved to be unreliable.
                        crashLoggers = if (isDebug) listOf(BeagleCrashLogger) else listOf()
                    )
                )
            )

            Beagle.set(
                HeaderModule(
                    title = androidContext().getString(R.string.app_name),
                    subtitle = BuildConfig.APPLICATION_ID,
                    text = "${BuildConfig.BUILD_TYPE} v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                ),
                AppInfoButtonModule(),
                DeveloperOptionsButtonModule(),
                PaddingModule(),
                TextModule("General", TextModule.Type.SECTION_HEADER),
                KeylineOverlaySwitchModule(),
                AnimationDurationSwitchModule(),
                ScreenCaptureToolboxModule(),
                DividerModule(),
                TextModule("Logs", TextModule.Type.SECTION_HEADER),
                LogListModule(maxItemCount = 25),
                DividerModule(),
                TextModule("Other", TextModule.Type.SECTION_HEADER),
                DeviceInfoModule(),
                BugReportButtonModule()
            )
        }

        Beagle
    }
}