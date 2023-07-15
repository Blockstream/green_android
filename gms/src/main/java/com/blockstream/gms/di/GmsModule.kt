package com.blockstream.gms.di

import com.blockstream.base.AppReview
import com.blockstream.base.ZendeskSdk
import com.blockstream.gms.AppReviewImpl
import com.blockstream.gms.BuildConfig
import com.blockstream.gms.ZendeskSdkImpl
import com.google.android.play.core.review.ReviewManagerFactory
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.dsl.binds
import org.koin.dsl.module


@Module
@ComponentScan("com.blockstream.green")
class GmsModule

val gmsModule = module {
    single {
        AppReviewImpl(ReviewManagerFactory.create(get()))
    } binds(arrayOf(AppReview::class))

    single {
        ZendeskSdkImpl(get(), BuildConfig.ZENDESK_CLIENT_ID)
    } binds(arrayOf(ZendeskSdk::class))
}