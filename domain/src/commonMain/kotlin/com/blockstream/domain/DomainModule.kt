package com.blockstream.domain

import com.blockstream.domain.account.accountModule
import com.blockstream.domain.banner.GetBannerUseCase
import com.blockstream.domain.bitcoinpricehistory.BitcoinPriceHistoryCache
import com.blockstream.domain.bitcoinpricehistory.ObserveBitcoinPriceHistory
import com.blockstream.domain.hardware.DisableHardwareWatchOnlyUseCase
import com.blockstream.domain.hardware.EnableHardwareWatchOnlyUseCase
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
import com.blockstream.domain.transaction.GetAccountTransactionsUseCase
import com.blockstream.domain.transaction.GetWalletTransactionsUseCase
import com.blockstream.domain.wallet.GetWalletAssetsUseCase
import com.blockstream.domain.wallet.walletModule
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val domainModule = module {
    includes(notificationsDomainModule)
    includes(meldDomainModule)
    includes(swapModule)
    includes(sendModule)
    includes(receiveModule)
    includes(walletModule)
    includes(accountModule)
    singleOf(::LightningNodeIdUseCase)
    singleOf(::VerifyAddressUseCase)
    singleOf(::EnableHardwareWatchOnlyUseCase)
    singleOf(::DisableHardwareWatchOnlyUseCase)
    singleOf(::CreateCryptoQuoteUseCase)
    singleOf(::CreateCryptoWidgetUseCase)
    singleOf(::DefaultValuesUseCase)
    singleOf(::MeldUseCase)
    singleOf(::GetBannerUseCase)
    singleOf(::GetPromoUseCase)
    factoryOf(::GetWalletTransactionsUseCase)
    factoryOf(::GetAccountTransactionsUseCase)
    singleOf(::BitcoinPriceHistoryCache)
    factoryOf(::ObserveBitcoinPriceHistory)
    factoryOf(::GetLastSuccessfulPurchaseExchange)
    factoryOf(::GetWalletAssetsUseCase)
}