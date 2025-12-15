package com.blockstream.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import com.blockstream.data.database.local.LocalDB
import com.blockstream.data.database.wallet.WalletDB

actual class DriverFactory {
    actual fun createWalletDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = WalletDB.Schema,
            name = DATABASE_NAME_WALLET,
            onConfiguration = { config ->
                config.copy(
                    extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = true)
                )
            }
        )
    }

    actual fun createLocalDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = LocalDB.Schema,
            name = DATABASE_NAME_LOCAL,
            onConfiguration = { config ->
                config.copy(
                    extendedConfig = DatabaseConfiguration.Extended(foreignKeyConstraints = true)
                )
            }
        )
    }
}