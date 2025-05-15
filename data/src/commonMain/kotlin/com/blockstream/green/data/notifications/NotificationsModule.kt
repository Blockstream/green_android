package com.blockstream.green.data.notifications

import com.blockstream.green.data.notifications.datasource.NotificationsRemoteDataSource
import org.koin.dsl.module

val notificationsDataModule = module {
    single { NotificationsRemoteDataSource(get()) }
    single { NotificationsRepository(get()) }
}