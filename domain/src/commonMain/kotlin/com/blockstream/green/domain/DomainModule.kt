package com.blockstream.green.domain

import com.blockstream.green.domain.meld.meldDomainModule
import com.blockstream.green.domain.notifications.notificationsDomainModule
import org.koin.dsl.module

val domainModule = module {
    includes(notificationsDomainModule)
    includes(meldDomainModule)
}