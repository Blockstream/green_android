package com.blockstream.data

import com.blockstream.data.json.DefaultJson
import com.blockstream.data.meld.meldModule
import com.blockstream.data.notifications.notificationsDataModule
import com.blockstream.data.walletabi.flow.FakeWalletAbiFlowDriver
import com.blockstream.data.walletabi.flow.WalletAbiFlowSnapshotStore
import com.blockstream.data.walletabi.request.DefaultWalletAbiDemoRequestSource
import com.blockstream.data.walletabi.request.WalletAbiDemoRequestSource
import com.blockstream.data.walletabi.request.NoOpWalletAbiDemoRequestOverrideStore
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

//expect val platformDataModule: Module

val dataModule = module {
    single { GreenWebhooksHttpClient(get()) }
    single { DefaultJson }
    singleOf(::WalletAbiFlowSnapshotStore)
    single { FakeWalletAbiFlowDriver() }
    single<WalletAbiDemoRequestSource> {
        DefaultWalletAbiDemoRequestSource(
            overrideStore = getOrNull() ?: NoOpWalletAbiDemoRequestOverrideStore
        )
    }
    includes(meldModule)
    includes(notificationsDataModule)

}
