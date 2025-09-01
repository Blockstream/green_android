package com.blockstream.green.domain.meld

import org.koin.dsl.module

val meldDomainModule = module {
    factory { GetMeldCountries(get()) }
    factory { GetPendingMeldTransactions(get()) }
}