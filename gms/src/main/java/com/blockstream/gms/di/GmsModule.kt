@file:OptIn(ExperimentalEncodingApi::class)

package com.blockstream.gms.di

import com.blockstream.base.GooglePlay
import com.blockstream.base.InstallReferrer
import com.blockstream.common.ZendeskSdk
import com.blockstream.common.data.AppConfig
import com.blockstream.common.fcm.Firebase
import com.blockstream.gms.FirebaseImpl
import com.blockstream.gms.GooglePlayImpl
import com.blockstream.gms.InstallReferrerImpl
import com.blockstream.gms.ZendeskSdkAndroid
import com.google.android.play.core.review.ReviewManagerFactory
import okio.internal.commonToUtf8String
import org.koin.core.annotation.Module
import org.koin.dsl.binds
import org.koin.dsl.module
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


@Module
class GmsModule

val gmsModule = module {
    single {
        GooglePlayImpl(ReviewManagerFactory.create(get()))
    } binds(arrayOf(GooglePlay::class))

    single {
        val apiKey = get<AppConfig>().zendeskClientId?.let { base64 ->
            Base64.decode(base64).commonToUtf8String()
        } ?: ""
        ZendeskSdkAndroid(get(), get(), apiKey)
    } binds(arrayOf(ZendeskSdk::class))

    single {
        FirebaseImpl(get())
    } binds(arrayOf(Firebase::class))

    single {
        InstallReferrerImpl(get(), get())
    } binds(arrayOf(InstallReferrer::class))
}