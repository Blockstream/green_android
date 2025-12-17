package com.blockstream.domain.send

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val sendModule = module {
    singleOf(::GetSendAssetsUseCase)
    singleOf(::GetSendAccountsUseCase)
    singleOf(::GetSendAmountUseCase)
    singleOf(::PrepareTransactionUseCase)
    singleOf(::GetSendFlowUseCase)
    singleOf(::ShowFeeSelectorUseCase)
    singleOf(::GetTransactionConfirmationUseCase)
    singleOf(::SendUseCase)
}
