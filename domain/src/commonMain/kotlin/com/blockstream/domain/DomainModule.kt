package com.blockstream.domain

import com.blockstream.domain.account.accountModule
import com.blockstream.domain.banner.GetBannerUseCase
import com.blockstream.domain.bitcoinpricehistory.ObserveBitcoinPriceHistory
import com.blockstream.domain.boltz.boltzModule
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
import com.blockstream.domain.wallet.walletModule
import org.koin.dsl.module

val domainModule = module {
    includes(notificationsDomainModule)
    includes(meldDomainModule)
    includes(boltzModule)
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
}