

package com.blockstream.green.di

import android.content.Context
import androidx.preference.PreferenceManager
import co.touchlab.kermit.Logger
import com.blockstream.common.CountlyBase
import com.blockstream.common.data.AppConfig
import com.blockstream.common.data.AppInfo
import com.blockstream.common.data.AppKeys
import com.blockstream.common.di.initKoinAndroid
import com.blockstream.gms.di.gmsModule
import com.blockstream.green.BuildConfig
import com.blockstream.green.R
import com.blockstream.green.data.Countly
import com.blockstream.green.data.CountlyAndroid
import com.blockstream.green.data.CountlyNoOp
import com.blockstream.green.utils.isDevelopmentFlavor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.binds
import org.koin.dsl.module
import org.koin.ksp.generated.com_blockstream_green_di_AndroidModule

fun startKoin(context: Context) {

    val appKeys: AppKeys? =
        context.resources.openRawResource(R.raw.app_keys).bufferedReader()
            .use { AppKeys.fromText(it.readText()) }

    val appConfig = AppConfig.default(
        isDebug = BuildConfig.DEBUG,
        gdkDataDir = context.filesDir.absolutePath,
        appKeys = appKeys,
        analyticsFeatureEnabled = context.resources.getBoolean(R.bool.feature_analytics),
        lightningFeatureEnabled = context.resources.getBoolean(R.bool.feature_lightning),
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
            single {
                if (context.resources.getBoolean(R.bool.feature_analytics)) {
                    Countly(get(), get(), get(), get(), get(), get())
                } else {
                    CountlyNoOp(get(), get(), get(), get())
                }
            } binds (arrayOf(CountlyBase::class, CountlyAndroid::class))
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