package com.blockstream.green.data

import com.blockstream.green.data.meld.meldModule
import com.blockstream.green.data.notifications.notificationsDataModule
import org.koin.dsl.module

//expect val platformDataModule: Module

val dataModule = module {
    single { GreenWebhooksHttpClient(get()) }

    includes(meldModule)
    includes(notificationsDataModule)

}