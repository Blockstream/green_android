package com.blockstream.domain.boltz

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val boltzModule = module {
    singleOf(::CreateReverseSubmarineSwapUseCase)
    singleOf(::CreateNormalSubmarineSwapUseCase)
    singleOf(::HandleSwapEventsUseCase)
    singleOf(::IsSwapsEnabledUseCase)
    singleOf(::GetWalletFromSwapUseCase)
    singleOf(::IsAddressSwappableUseCase)
    singleOf(::BoltzUseCase)
}
