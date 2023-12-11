@file:OptIn(ExperimentalEncodingApi::class)

package com.blockstream.gms.di

import com.blockstream.base.AppReview
import com.blockstream.base.ZendeskSdk
import com.blockstream.common.data.AppConfig
import com.blockstream.gms.AppReviewImpl
import com.blockstream.gms.ZendeskSdkImpl
import com.google.android.play.core.review.ReviewManagerFactory
import okio.internal.commonToUtf8String
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.dsl.binds
import org.koin.dsl.module
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


@Module
@ComponentScan("com.blockstream.green")
class GmsModule

val gmsModule = module {
    single {
        AppReviewImpl(ReviewManagerFactory.create(get()))
    } binds(arrayOf(AppReview::class))

    single {
        val apiKey = get<AppConfig>().zendeskClientId?.let { base64 ->
            Base64.decode(base64).commonToUtf8String()
        } ?: ""
        ZendeskSdkImpl(get(), apiKey)
    } binds(arrayOf(ZendeskSdk::class))
}