package com.blockstream.data.meld

import org.koin.dsl.module

val meldModule = module {
    single {
        _root_ide_package_.com.blockstream.data.meld.MeldHttpClient(get())
    }
    single {
        _root_ide_package_.com.blockstream.data.meld.datasource.MeldRemoteDataSource(get())
    }
    single {
        _root_ide_package_.com.blockstream.data.meld.datasource.MeldLocalDataSource()
    }
    single {
        _root_ide_package_.com.blockstream.data.meld.MeldRepository(get(), get())
    }
}