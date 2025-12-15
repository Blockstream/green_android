package com.blockstream.domain.notifications

import org.koin.dsl.module

val notificationsDomainModule = module {
    factory { RegisterFCMToken(get()) }
}