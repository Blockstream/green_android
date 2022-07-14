package com.blockstream.green.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `wallets` ADD COLUMN `wallet_hash_id` TEXT NOT NULL DEFAULT ''");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_wallets_wallet_hash_id` ON `wallets` (`wallet_hash_id`)");
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `wallets` ADD COLUMN `ask_bip39_passphrase` INTEGER NOT NULL DEFAULT 0")
    }
}