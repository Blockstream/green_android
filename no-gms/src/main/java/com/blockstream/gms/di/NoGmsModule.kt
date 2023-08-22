package com.blockstream.gms.di

import com.blockstream.base.AppReview
import com.blockstream.base.ZendeskSdk
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class NoGmsModule {

    @Singleton
    @Provides
    fun provideAppReview(): AppReview {
        return AppReview()
    }

    @Singleton
    @Provides
    fun provideZendeskSdk(): ZendeskSdk {
        return ZendeskSdk()
    }
}