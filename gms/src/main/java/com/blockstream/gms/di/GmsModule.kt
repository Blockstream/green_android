@file:OptIn(ExperimentalEncodingApi::class)

package com.blockstream.gms.di

import com.blockstream.base.AppReview
import com.blockstream.base.Firebase
import com.blockstream.common.ZendeskSdk
import com.blockstream.common.data.AppConfig
import com.blockstream.gms.AppReviewImpl
import com.blockstream.gms.FirebaseImpl
import com.blockstream.gms.ZendeskSdkAndroid
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
        ZendeskSdkAndroid(get(), apiKey)
    } binds(arrayOf(ZendeskSdk::class))

    single {
        FirebaseImpl(get())
    } binds(arrayOf(Firebase::class))
}