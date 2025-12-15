package com.blockstream.data

import com.blockstream.data.json.DefaultJson
import com.blockstream.data.meld.meldModule
import com.blockstream.data.notifications.notificationsDataModule
import org.koin.dsl.module

//expect val platformDataModule: Module

val dataModule = module {
    single { GreenWebhooksHttpClient(get()) }
    single { DefaultJson }
    includes(meldModule)
    includes(notificationsDataModule)

}
