package com.blockstream.green.data

import com.blockstream.green.data.meld.meldModule
import org.koin.dsl.module

//expect val platformDataModule: Module

val dataModule = module {
    includes(meldModule)
}