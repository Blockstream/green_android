package com.blockstream.data.notifications

import com.blockstream.data.notifications.datasource.NotificationsRemoteDataSource
import org.koin.dsl.module

val notificationsDataModule = module {
    single { NotificationsRemoteDataSource(get()) }
    single { NotificationsRepository(get()) }
}