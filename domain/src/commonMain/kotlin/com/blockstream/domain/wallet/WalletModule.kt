package com.blockstream.domain.wallet

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val walletModule = module {
    singleOf(::NewWalletUseCase)
    singleOf(::RestoreWalletUseCase)
    singleOf(::SaveDerivedLightningMnemonicUseCase)
    singleOf(::SaveDerivedBoltzMnemonicUseCase)
}
