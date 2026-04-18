package com.blockstream.data

import com.blockstream.data.json.DefaultJson
import com.blockstream.data.meld.meldModule
import com.blockstream.data.notifications.notificationsDataModule
import com.blockstream.data.walletabi.flow.FakeWalletAbiFlowDriver
import com.blockstream.data.walletabi.flow.WalletAbiFlowSnapshotStore
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

//expect val platformDataModule: Module

val dataModule = module {
    single { GreenWebhooksHttpClient(get()) }
    single { DefaultJson }
    singleOf(::WalletAbiFlowSnapshotStore)
    singleOf(::FakeWalletAbiFlowDriver)
    includes(meldModule)
    includes(notificationsDataModule)

}
