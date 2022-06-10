package com.blockstream.green.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.blockstream.green.utils.isDevelopmentFlavor

@Database(
    entities = [Wallet::class, LoginCredentials::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val builder = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                com.blockstream.green.Database.DATABASE_NAME
            )

            builder.addMigrations(MIGRATION_1_2).build()

            if (isDevelopmentFlavor) {
                builder.fallbackToDestructiveMigration()
            }

            return builder.build()
        }
    }
}
