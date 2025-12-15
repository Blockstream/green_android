package com.blockstream.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.blockstream.data.database.local.LocalDB
import com.blockstream.data.database.wallet.WalletDB

actual class DriverFactory {
    actual fun createWalletDriver(): SqlDriver {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WalletDB.Schema.create(driver)
        return driver
    }

    actual fun createLocalDriver(): SqlDriver {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LocalDB.Schema.create(driver)
        return driver
    }
}