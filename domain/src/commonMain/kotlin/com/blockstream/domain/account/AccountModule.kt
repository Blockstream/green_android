package com.blockstream.domain.account

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val accountModule = module {
    singleOf(::CreateAccountUseCase)
    singleOf(::RemoveAccountUseCase)
}
