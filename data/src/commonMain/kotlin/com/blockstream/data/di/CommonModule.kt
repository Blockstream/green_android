package com.blockstream.data.di

import com.blockstream.data.dataModule
import com.blockstream.data.usecases.CheckRecoveryPhraseUseCase
import com.blockstream.data.usecases.EnableHardwareWatchOnlyUseCase
import com.blockstream.data.usecases.SetBiometricsUseCase
import com.blockstream.data.usecases.SetPinUseCase
import com.blockstream.data.utils.WatchOnlyDetector
import org.koin.dsl.module

//At some point we'll move this to domain module.
val commonModule = module {
    includes(dataModule)
//    includes(domainModule)
//    includes(boltzModule)
//    includes(sendModule)
//    includes(receiveModule)
//    includes(walletModule)
//    includes(accountModule)
    single {
        WatchOnlyDetector(get())
    }
//    single {
//        LightningNodeIdUseCase(get())
//    }
//    single {
//        VerifyAddressUseCase(get())
//    }
//    single {
//        CreateCryptoQuoteUseCase(get())
//    }
//    single {
//        CreateCryptoWidgetUseCase(get())
//    }
//    single {
//        DefaultValuesUseCase(get())
//    }
//    single {
//        MeldUseCase(get(), get(), get())
//    }
    single {
        CheckRecoveryPhraseUseCase(get())
    }
    single {
        SetBiometricsUseCase(get(), get())
    }
    single {
        SetPinUseCase(get())
    }
    single {
        EnableHardwareWatchOnlyUseCase(get(), get())
    }
//    factory {
//        ObserveBitcoinPriceHistory(get())
//    }
//    factory {
//        GetLastSuccessfulPurchaseExchange(get())
//    }
//    single {
//        GetBannerUseCase()
//    }
//    single {
//        GetPromoUseCase(get(), get(), get())
//    }
}
