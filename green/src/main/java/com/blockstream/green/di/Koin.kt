

package com.blockstream.green.di

import android.content.Context
import androidx.preference.PreferenceManager
import co.touchlab.kermit.Logger
import com.blockstream.common.data.AppConfig
import com.blockstream.common.data.AppInfo
import com.blockstream.common.di.initKoinAndroid
import com.blockstream.gms.di.gmsModule
import com.blockstream.green.BuildConfig
import com.blockstream.green.R
import com.blockstream.green.utils.isDevelopmentFlavor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.ksp.generated.com_blockstream_green_di_AndroidModule

fun startKoin(context: Context) {

    val appConfig = AppConfig(
        isDebug = BuildConfig.DEBUG,
        gdkDataDir = context.filesDir.absolutePath,
        greenlightApiKey = BuildConfig.BREEZ_API_KEY,
        greenlightKey = BuildConfig.GREENLIGHT_DEVICE_KEY,
        greenlightCert = BuildConfig.GREENLIGHT_DEVICE_CERT,
        analyticsFeatureEnabled = context.resources.getBoolean(R.bool.feature_analytics),
        lightningFeatureEnabled = context.resources.getBoolean(R.bool.feature_lightning) && BuildConfig.BREEZ_API_KEY.isNotBlank(),
        storeRateEnabled = context.resources.getBoolean(R.bool.feature_rate_google_play)
    )

    initKoinAndroid(
        appConfig,
        AppInfo(
            userAgent = "green_android",
            version = BuildConfig.VERSION_NAME,
            isDebug = BuildConfig.DEBUG,
            isDevelopment = isDevelopmentFlavor
        ),
        module {
            single {
                context
            }
            single {
                PreferenceManager.getDefaultSharedPreferences(androidContext())
            }
        },
        com_blockstream_green_di_AndroidModule,
        databaseModule,
        greenModules,
        viewModels,
        gmsModule
    ) {
        Logger.d { "Starting up (Android)" }
    }
}