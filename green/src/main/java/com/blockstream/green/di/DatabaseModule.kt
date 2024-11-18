package com.blockstream.green.di

import com.blockstream.green.database.AppDatabase
import org.koin.dsl.module

val databaseModule = module {
    single {
        AppDatabase.getInstance(get()).walletDao()
    }
}