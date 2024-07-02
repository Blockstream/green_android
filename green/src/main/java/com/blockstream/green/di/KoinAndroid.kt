package com.blockstream.green.di

import android.content.Context
import com.blockstream.common.CountlyBase
import com.blockstream.common.data.AppConfig
import com.blockstream.common.data.AppInfo
import com.blockstream.common.di.initKoin
import com.blockstream.gms.di.gmsModule
import com.blockstream.green.BuildConfig
import com.blockstream.green.R
import com.blockstream.green.data.Countly
import com.blockstream.green.data.CountlyAndroid
import com.blockstream.green.data.CountlyNoOp
import com.blockstream.green.utils.isDevelopmentFlavor
import org.koin.dsl.binds
import org.koin.dsl.module
import org.koin.ksp.generated.com_blockstream_green_di_AndroidModule

fun initKoinAndroid(context: Context, doOnStartup: () -> Unit = {}) {
    val appInfo = AppInfo(
        userAgent = "green_android",
        version = BuildConfig.VERSION_NAME,
        isDebug = BuildConfig.DEBUG,
        isDevelopment = isDevelopmentFlavor
    )

    val appConfig = AppConfig.default(
        isDebug = BuildConfig.DEBUG,
        filesDir = context.filesDir.absolutePath,
        cacheDir = context.cacheDir.absolutePath,
        analyticsFeatureEnabled = context.resources.getBoolean(R.bool.feature_analytics),
        lightningFeatureEnabled = context.resources.getBoolean(R.bool.feature_lightning),
        storeRateEnabled = context.resources.getBoolean(R.bool.feature_rate_google_play)
    )

    initKoin(
        appInfo = appInfo,
        appConfig = appConfig,
        doOnStartup = doOnStartup,
        module {
            single {
                context
            }
            single {
                if (context.resources.getBoolean(R.bool.feature_analytics)) {
                    Countly(get(), get(), get(), get(), get(), get(), get())
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
    )
}