package com.blockstream.gms.di

import android.content.Context
import com.blockstream.base.AppReview
import com.blockstream.gms.AppReviewImpl
import com.blockstream.gms.BuildConfig
import com.blockstream.gms.ZendeskSdkImpl
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class GmsModule {
    @Singleton
    @Provides
    fun provideReviewManager(@ApplicationContext context: Context): ReviewManager {
        return ReviewManagerFactory.create(context)
    }

    @Singleton
    @Provides
    fun provideAppReview(reviewManager: ReviewManager): AppReview {
        return AppReviewImpl(reviewManager)
    }

    @Singleton
    @Provides
    fun provideZendeskSdk(@ApplicationContext context: Context): com.blockstream.base.ZendeskSdk {
        return ZendeskSdkImpl(context, BuildConfig.ZENDESK_CLIENT_ID)
    }
}