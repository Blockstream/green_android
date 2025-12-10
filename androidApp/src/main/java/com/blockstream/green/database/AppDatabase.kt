package com.blockstream.green.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.DeviceIdentifier
import com.blockstream.common.data.EncryptedData
import com.blockstream.common.gdk.JsonConverter.Companion.JsonDeserializer
import com.blockstream.common.gdk.data.PinData

@Database(
    entities = [Wallet::class, LoginCredentials::class],
    version = 6,
    exportSchema = true
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
                "green.db"
            )

            builder.addMigrations(MIGRATION_1_2).build()
            builder.addMigrations(MIGRATION_2_3).build()
            builder.addMigrations(MIGRATION_3_4).build()
            builder.addMigrations(MIGRATION_4_5).build()
            builder.addMigrations(MIGRATION_5_6).build()

            return builder.build()
        }
    }
}

class Converters {
    @TypeConverter
    fun toType(value: Int) = enumValues<CredentialType>()[value]

    @TypeConverter
    fun fromType(value: CredentialType) = value.ordinal

    @TypeConverter
    fun toPinData(value: String?): PinData? = value?.let {
        JsonDeserializer.decodeFromString(it)
    }

    @TypeConverter
    fun fromPinData(value: PinData?): String? = value?.let { JsonDeserializer.encodeToString(it) }

    @TypeConverter
    fun toEncryptedData(value: String?): EncryptedData? = value?.let {
        JsonDeserializer.decodeFromString(it)
    }

    @TypeConverter
    fun fromEncryptedData(value: EncryptedData?): String? = value?.let {
        JsonDeserializer.encodeToString(it)
    }

    @TypeConverter
    fun toDeviceIdentifierList(value: String?): List<DeviceIdentifier>? = value?.let {
        JsonDeserializer.decodeFromString(it)
    }

    @TypeConverter
    fun fromDeviceIdentifierList(value: List<DeviceIdentifier>?): String? = value?.let { JsonDeserializer.encodeToString(it) }
}

