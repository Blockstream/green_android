package com.blockstream.domain.swap

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin module definition for swap-related domain use cases.
 */
val swapModule = module {
    singleOf(::CreateReverseSubmarineSwapUseCase)
    singleOf(::CreateNormalSubmarineSwapUseCase)
    singleOf(::CreateChainSwapUseCase)
    singleOf(::CreateSwapUseCase)
    singleOf(::GetSwappableAccountsUseCase)
    singleOf(::HandleSwapEventsUseCase)
    singleOf(::IsSwapsEnabledUseCase)
    singleOf(::GetWalletFromSwapUseCase)
    singleOf(::IsInvoiceSwappableUseCase)
    singleOf(::IsLiquidToLightningSwapUseCase)
    singleOf(::PrepareSwapTransactionUseCase)
    singleOf(::IsSwapAvailableUseCase)
    singleOf(::GetQuoteUseCase)
    singleOf(::SwapUseCase)
    singleOf(::GetSwapAmountUseCase)
}
