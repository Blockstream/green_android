package com.blockstream.domain.receive

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val receiveModule = module {
    singleOf(::GetReceiveAssetsUseCase)
    singleOf(::GetReceiveAccountsUseCase)
    singleOf(::ReceiveUseCase)
    factoryOf(::GetReceiveAmountUseCase)
}
