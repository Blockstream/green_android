package com.blockstream.green.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.Wally
import com.blockstream.common.gdk.getGdkBinding
import com.blockstream.common.gdk.getWally
import com.blockstream.common.gdk.params.InitConfig
import com.blockstream.common.managers.AssetManager
import com.blockstream.common.managers.LifecycleManager
import com.blockstream.common.managers.SettingsManager
import com.blockstream.green.ApplicationScope
import com.blockstream.green.BuildConfig
import com.blockstream.green.GreenApplication
import com.blockstream.green.R
import com.blockstream.green.data.Countly
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.lifecycle.ActivityLifecycle
import com.blockstream.green.managers.NotificationManager
import com.blockstream.green.managers.SessionManager
import com.blockstream.green.settings.Migrator
import com.blockstream.green.utils.*
import com.blockstream.lightning.LightningManager
import com.pandulapeter.beagle.Beagle
import com.pandulapeter.beagle.common.configuration.Behavior
import com.pandulapeter.beagle.logCrash.BeagleCrashLogger
import com.pandulapeter.beagle.modules.*
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.MainScope
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class GreenModules {
    @Singleton
    @Provides
    fun provideApplicationScope(): ApplicationScope {
        return MainScope()
    }

    @Singleton
    @Provides
    fun provideSettings(sharedPreferences: SharedPreferences): Settings {
        return SharedPreferencesSettings(sharedPreferences)
    }

    @Singleton
    @Provides
    fun provideLifecycleManager(): LifecycleManager {
        return LifecycleManager()
    }

    @Singleton
    @Provides
    fun provideGDK(@ApplicationContext context: Context, settings: Settings): Gdk {
        val config = InitConfig(
            datadir = context.filesDir.absolutePath,
            logLevel = if (BuildConfig.DEBUG) "debug" else "none",
            enableSinglesigLiquidHWW = true
        )

        return Gdk(
            settings = settings,
            gdkBinding = getGdkBinding(isDevelopmentFlavor, config)
        )
    }

    @Singleton
    @Provides
    fun provideKotlinWally(): Wally {
        return getWally()
    }

    @Singleton
    @Provides
    fun provideSessionManager(
        applicationScope: ApplicationScope,
        lifecycleManager: LifecycleManager,
        lightningManager: LightningManager,
        settingsManager: SettingsManager,
        assetManager: AssetManager,
        countly: Countly,
        gdk: Gdk,
        wally: Wally,
        qaTester: QATester
    ): SessionManager {
        return SessionManager(applicationScope, lifecycleManager, lightningManager, settingsManager, assetManager, countly, gdk, wally, qaTester)
    }

    @Singleton
    @Provides
    fun provideLightningManager(): LightningManager {
        return LightningManager()
    }

    @Singleton
    @Provides
    fun provideAssetManager(applicationScope: ApplicationScope, QATester: QATester): AssetManager {
        return AssetManager(applicationScope, QATester)
    }

    @Singleton
    @Provides
    fun provideCountly(@ApplicationContext context: Context, sharedPreferences: SharedPreferences, applicationScope: ApplicationScope, settingsManager: SettingsManager, walletRepository: WalletRepository): Countly {
        return Countly(context, sharedPreferences, applicationScope, settingsManager, walletRepository)
    }

    @Singleton
    @Provides
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        val sharedPreferences = context.getSharedPreferences(SettingsManager.APPLICATION_SETTINGS_NAME, Context.MODE_PRIVATE)
        val settings = SharedPreferencesSettings(sharedPreferences)

        return SettingsManager(
            analyticsFeatureEnabled = context.resources.getBoolean(R.bool.feature_analytics),
            lightningFeatureEnabled = context.resources.getBoolean(R.bool.feature_lightning) && com.blockstream.crypto.BuildConfig.BREEZ_API_KEY.isNotBlank(),
            rateGooglePlayEnabled = context.resources.getBoolean(R.bool.feature_rate_google_play),
            settings = settings
        )
    }

    @Singleton
    @Provides
    fun provideAppKeystore(): AppKeystore {
        return AppKeystore()
    }

    @Singleton
    @Provides
    fun provideMigrator(@ApplicationContext context: Context, sharedPreferences: SharedPreferences, walletRepository: WalletRepository, gdk: Gdk, settingsManager: SettingsManager, applicationScope: ApplicationScope): Migrator {
        return Migrator(context, sharedPreferences, walletRepository, gdk, settingsManager, applicationScope)
    }

    @Singleton
    @Provides
    fun provideQATester(@ApplicationContext context: Context): QATester {
        return QATester(context)
    }

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Singleton
    @Provides
    fun provideAndroidNotificationManager(@ApplicationContext context: Context): android.app.NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    }

    @Singleton
    @Provides
    fun provideBluetoothManager(@ApplicationContext context: Context): BluetoothAdapter? {
        return (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    @Singleton
    @Provides
    fun provideNotificationManager(
        @ApplicationContext context: Context,
        applicationScope: ApplicationScope,
        androidNotificationManager: android.app.NotificationManager,
        sessionManager: SessionManager,
        settingsManager: SettingsManager,
        walletRepository: WalletRepository,
        countly: Countly,
    ): NotificationManager {
        return NotificationManager(
            context,
            applicationScope,
            androidNotificationManager,
            sessionManager,
            settingsManager,
            walletRepository,
            countly
        )
    }

    @Singleton
    @Provides
    fun provideBeagle(@ApplicationContext context: Context): Beagle {

        if (isDevelopmentOrDebug) {
            Beagle.initialize(
                application = context as GreenApplication,
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
                    title = context.getString(R.string.app_name),
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

        return Beagle
    }

    @Singleton
    @Provides
    fun provideActivityLifecycle(
        sessionManager: SessionManager,
        notificationManager: NotificationManager
    ): ActivityLifecycle {
        return ActivityLifecycle(sessionManager, notificationManager)
    }
}