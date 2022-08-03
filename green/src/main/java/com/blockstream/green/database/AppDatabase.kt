package com.blockstream.green.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import com.blockstream.DeviceBrand
import com.blockstream.base.Database.DATABASE_NAME
import com.blockstream.gdk.GdkBridge
import com.blockstream.gdk.data.PinData
import com.blockstream.green.utils.EncryptedData
import com.blockstream.green.utils.isDevelopmentFlavor
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@Database(
    entities = [Wallet::class, LoginCredentials::class],
    version = 4,
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
                DATABASE_NAME
            )

            builder.addMigrations(MIGRATION_1_2).build()
            builder.addMigrations(MIGRATION_2_3).build()
            builder.addMigrations(MIGRATION_3_4).build()

            if (isDevelopmentFlavor) {
                 // builder.fallbackToDestructiveMigration()
            }

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
        GdkBridge.JsonDeserializer.decodeFromString(it)
    }

    @TypeConverter
    fun fromPinData(value: PinData?): String? = value?.let { GdkBridge.JsonDeserializer.encodeToString(it)}

    @TypeConverter
    fun toEncryptedData(value: String?): EncryptedData? = value?.let {
        GdkBridge.JsonDeserializer.decodeFromString(it)
    }

    @TypeConverter
    fun fromEncryptedData(value: EncryptedData?): String? = value?.let {
        GdkBridge.JsonDeserializer.encodeToString(it)}

    @TypeConverter
    fun toDeviceIdentifierList(value: String?): List<DeviceIdentifier>? = value?.let {
        GdkBridge.JsonDeserializer.decodeFromString(it)
    }

    @TypeConverter
    fun fromDeviceIdentifierList(value: List<DeviceIdentifier>?): String? = value?.let { GdkBridge.JsonDeserializer.encodeToString(it)}

    @TypeConverter
    fun toDeviceBrand(value: Int) = enumValues<DeviceBrand>()[value]

    @TypeConverter
    fun fromDeviceBrand(value: DeviceBrand) = value.ordinal
}

