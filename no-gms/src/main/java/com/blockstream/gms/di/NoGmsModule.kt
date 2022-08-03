package com.blockstream.gms.di

import com.blockstream.base.IAppReview
import com.blockstream.gms.NoAppReview
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
    fun provideAppReview(): IAppReview {
        return NoAppReview()
    }
}