package com.blockstream.data.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.blockstream.data.database.local.LocalDB
import com.blockstream.data.database.wallet.WalletDB

actual class DriverFactory(private val context: Context) {
    actual fun createWalletDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = WalletDB.Schema,
            context = context,
            name = DATABASE_NAME_WALLET,
            callback = object : AndroidSqliteDriver.Callback(WalletDB.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                }
            }
        )
    }

    actual fun createLocalDriver(): SqlDriver {
        val driver = AndroidSqliteDriver(
            schema = LocalDB.Schema,
            context = context,
            name = DATABASE_NAME_LOCAL,
            callback = object : AndroidSqliteDriver.Callback(LocalDB.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                }
            }
        )
        return driver
    }
}