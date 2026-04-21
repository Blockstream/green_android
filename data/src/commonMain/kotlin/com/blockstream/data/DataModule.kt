package com.blockstream.data

import com.blockstream.data.json.DefaultJson
import com.blockstream.data.meld.meldModule
import com.blockstream.data.notifications.notificationsDataModule
import com.blockstream.data.walletabi.flow.FakeWalletAbiFlowDriver
import com.blockstream.data.walletabi.flow.WalletAbiFlowSnapshotStore
import com.blockstream.data.walletabi.provider.WalletAbiEsploraHttpClient
import com.blockstream.data.walletabi.provider.WalletAbiJadePsetSignerFactory
import com.blockstream.data.walletabi.request.DefaultWalletAbiDemoRequestSource
import com.blockstream.data.walletabi.request.WalletAbiDemoRequestSource
import com.blockstream.data.walletabi.request.NoOpWalletAbiDemoRequestOverrideStore
import com.blockstream.data.walletabi.walletconnect.WalletAbiWalletConnectManager
import com.blockstream.data.walletabi.walletconnect.WalletAbiWalletConnectManaging
import com.blockstream.data.walletabi.walletconnect.WalletAbiWalletConnectSnapshotStore
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

//expect val platformDataModule: Module

val dataModule = module {
    single { GreenWebhooksHttpClient(get()) }
    single { DefaultJson }
    singleOf(::WalletAbiFlowSnapshotStore)
    singleOf(::WalletAbiWalletConnectSnapshotStore)
    singleOf(::WalletAbiEsploraHttpClient)
    single<WalletAbiWalletConnectManaging> {
        WalletAbiWalletConnectManager(
            applicationScope = get(),
            snapshotStore = get(),
            bridge = get(),
            esploraHttpClient = get(),
            jadePsetSignerFactory = getOrNull() ?: WalletAbiJadePsetSignerFactory.Device,
        )
    }
    single { FakeWalletAbiFlowDriver() }
    single<WalletAbiDemoRequestSource> {
        DefaultWalletAbiDemoRequestSource(
            overrideStore = getOrNull() ?: NoOpWalletAbiDemoRequestOverrideStore
        )
    }
    includes(meldModule)
    includes(notificationsDataModule)

}
