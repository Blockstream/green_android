package com.blockstream.domain

import com.blockstream.domain.account.accountModule
import com.blockstream.domain.banner.GetBannerUseCase
import com.blockstream.domain.bitcoinpricehistory.ObserveBitcoinPriceHistory
import com.blockstream.domain.hardware.VerifyAddressUseCase
import com.blockstream.domain.lightning.LightningNodeIdUseCase
import com.blockstream.domain.meld.CreateCryptoQuoteUseCase
import com.blockstream.domain.meld.CreateCryptoWidgetUseCase
import com.blockstream.domain.meld.DefaultValuesUseCase
import com.blockstream.domain.meld.GetLastSuccessfulPurchaseExchange
import com.blockstream.domain.meld.MeldUseCase
import com.blockstream.domain.meld.meldDomainModule
import com.blockstream.domain.notifications.notificationsDomainModule
import com.blockstream.domain.promo.GetPromoUseCase
import com.blockstream.domain.receive.receiveModule
import com.blockstream.domain.send.sendModule
import com.blockstream.domain.swap.swapModule
import com.blockstream.domain.wallet.walletModule
import com.blockstream.domain.walletabi.execution.DefaultWalletAbiExecutionRunner
import com.blockstream.domain.walletabi.execution.DefaultWalletAbiExecutionPlanner
import com.blockstream.domain.walletabi.execution.DefaultWalletAbiReviewPreviewer
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionRunner
import com.blockstream.domain.walletabi.execution.WalletAbiExecutionPlanner
import com.blockstream.domain.walletabi.execution.WalletAbiReviewPreviewer
import com.blockstream.domain.walletabi.flow.DefaultWalletAbiFlowStore
import com.blockstream.domain.walletabi.flow.WalletAbiFlowSnapshotRepository
import com.blockstream.domain.walletabi.flow.WalletAbiFlowStore
import org.koin.dsl.module

val domainModule = module {
    includes(notificationsDomainModule)
    includes(meldDomainModule)
    includes(swapModule)
    includes(sendModule)
    includes(receiveModule)
    includes(walletModule)
    includes(accountModule)
    single {
        LightningNodeIdUseCase(get())
    }
    single {
        VerifyAddressUseCase(get())
    }
    single {
        CreateCryptoQuoteUseCase(get())
    }
    single {
        CreateCryptoWidgetUseCase(get())
    }
    single {
        DefaultValuesUseCase(get())
    }
    single {
        MeldUseCase(get(), get(), get())
    }
    factory {
        ObserveBitcoinPriceHistory(get())
    }
    factory {
        GetLastSuccessfulPurchaseExchange(get())
    }
    single {
        GetBannerUseCase()
    }
    single {
        GetPromoUseCase(get(), get(), get())
    }
    factory<WalletAbiFlowStore> {
        DefaultWalletAbiFlowStore()
    }
    factory<WalletAbiExecutionPlanner> {
        DefaultWalletAbiExecutionPlanner()
    }
    factory<WalletAbiExecutionRunner> {
        DefaultWalletAbiExecutionRunner()
    }
    factory<WalletAbiReviewPreviewer> {
        DefaultWalletAbiReviewPreviewer(get())
    }
    single {
        WalletAbiFlowSnapshotRepository(get())
    }
}
