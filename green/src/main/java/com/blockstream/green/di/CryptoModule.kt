package com.blockstream.green.di

import android.content.Context
import com.blockstream.gdk.GreenWallet
import com.blockstream.green.gdk.SessionManager
import com.blockstream.green.lifecycle.AppLifecycleObserver
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.utils.AppKeystore
import com.blockstream.green.utils.AssetManager
import com.blockstream.libgreenaddress.KotlinGDK
import com.blockstream.libwally.KotlinWally
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class CryptoModule {
    @Singleton
    @Provides
    fun provideKotlinGDK(): KotlinGDK {
        return KotlinGDK()
    }

    @Singleton
    @Provides
    fun provideKotlinWally(): KotlinWally {
        return KotlinWally()
    }

    @Singleton
    @Provides
    fun provideGreenWallet(
        @ApplicationContext context: Context,
        gdk: KotlinGDK,
        wally: KotlinWally
    ): GreenWallet {
        return GreenWallet(gdk, wally, context.filesDir.absolutePath)
    }

    @Singleton
    @Provides
    fun provideSessionManager(
        settingsManager: SettingsManager,
        assetManager: AssetManager,
        greenWallet: GreenWallet
    ): SessionManager {
        return SessionManager(settingsManager, assetManager, greenWallet)
    }

    @Singleton
    @Provides
    fun provideAssetManager(): AssetManager {
        return AssetManager()
    }

    @Singleton
    @Provides
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Singleton
    @Provides
    fun provideAppKeystore(): AppKeystore {
        return AppKeystore()
    }

    @Singleton
    @Provides
    fun provideAppLifecycleObserver(sessionManager: SessionManager): AppLifecycleObserver {
        return AppLifecycleObserver(sessionManager)
    }
}