package com.blockstream.common.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.blockstream.common.database.wallet.WalletDB

actual class DriverFactory {
    actual fun createWalletDriver(): SqlDriver {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WalletDB.Schema.create(driver)
        return driver
    }

    actual fun createLocalDriver(): SqlDriver {
        val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WalletDB.Schema.create(driver)
        return driver
    }
}