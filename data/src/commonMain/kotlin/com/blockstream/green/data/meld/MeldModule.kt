package com.blockstream.green.data.meld

import org.koin.dsl.module

val meldModule = module {
    single {
        MeldHttpClient(get())
    }
    single {
        MeldRepository(get())
    }
}