package com.blockstream.gms.di

import com.blockstream.base.GooglePlay
import com.blockstream.base.InstallReferrer
import com.blockstream.data.ZendeskSdk
import com.blockstream.data.fcm.Firebase
import org.koin.dsl.module

val gmsModule = module {
    single {
        GooglePlay()
    }
    single {
        ZendeskSdk()
    }
    single {
        Firebase()
    }
    single {
        InstallReferrer()
    }
}