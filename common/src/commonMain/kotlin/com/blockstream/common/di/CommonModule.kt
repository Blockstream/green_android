package com.blockstream.common.di

import com.blockstream.common.usecases.CheckRecoveryPhraseUseCase
import com.blockstream.common.usecases.CreateAccountUseCase
import com.blockstream.common.usecases.EnableHardwareWatchOnlyUseCase
import com.blockstream.common.usecases.NewWalletUseCase
import com.blockstream.common.usecases.RestoreWalletUseCase
import com.blockstream.common.usecases.SetBiometricsUseCase
import com.blockstream.common.usecases.SetPinUseCase
import com.blockstream.domain.bitcoinpricehistory.ObserveBitcoinPriceHistory
import com.blockstream.domain.hardware.VerifyAddressUseCase
import com.blockstream.domain.meld.CreateCryptoQuoteUseCase
import com.blockstream.domain.meld.CreateCryptoWidgetUseCase
import com.blockstream.domain.meld.DefaultValuesUseCase
import com.blockstream.domain.meld.GetLastSuccessfulPurchaseExchange
import com.blockstream.domain.meld.MeldUseCase
import com.blockstream.domain.navigation.NavigateToWallet
import com.blockstream.green.data.dataModule
import com.blockstream.green.domain.domainModule
import org.koin.dsl.module

//At some point we'll move this to domain module. 
val commonModule = module {
    includes(dataModule)
    includes(domainModule)
    single {
        NavigateToWallet(get(), get())
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
    single {
        NewWalletUseCase(get(), get(), get(), get(), get(), get(), get())
    }
    single {
        RestoreWalletUseCase(get(), get(), get(), get(), get(), get(), get())
    }
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
    single {
        CreateAccountUseCase(get(), get(), get())
    }

    factory {
        ObserveBitcoinPriceHistory(get())
    }

    factory {
        GetLastSuccessfulPurchaseExchange(get())
    }
}