package com.blockstream.green.domain.notifications

import org.koin.dsl.module

val notificationsDomainModule = module {
    factory { RegisterFCMToken(get()) }
}