package com.blockstream.gms.di

import com.blockstream.base.GooglePlay
import com.blockstream.base.InstallReferrer
import com.blockstream.data.fcm.Firebase
import com.blockstream.gms.FirebaseImpl
import com.blockstream.gms.GooglePlayImpl
import com.blockstream.gms.InstallReferrerImpl
import com.google.android.play.core.review.ReviewManagerFactory
import org.koin.dsl.binds
import org.koin.dsl.module

val gmsModule = module {
    single {
        GooglePlayImpl(ReviewManagerFactory.create(get()))
    } binds (arrayOf(GooglePlay::class))

    single {
        FirebaseImpl(get())
    } binds (arrayOf(Firebase::class))

    single {
        InstallReferrerImpl(get(), get())
    } binds (arrayOf(InstallReferrer::class))
}