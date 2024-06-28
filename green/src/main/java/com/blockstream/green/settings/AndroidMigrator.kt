package com.blockstream.green.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.blockstream.base.Preferences
import com.blockstream.common.data.CredentialType
import com.blockstream.common.data.EncryptedData
import com.blockstream.common.data.toGreenWallet
import com.blockstream.common.database.Database
import com.blockstream.common.gdk.Gdk
import com.blockstream.common.gdk.data.PinData
import com.blockstream.common.managers.SettingsManager
import com.blockstream.common.utils.toHex
import com.blockstream.green.database.LoginCredentials
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletDao
import com.blockstream.green.database.roomToDelight
import kotlinx.coroutines.runBlocking
import com.blockstream.common.utils.Loggable
import org.koin.core.annotation.Single
import java.security.KeyStore
import java.security.UnrecoverableKeyException

class AndroidMigrator(
    val context: Context,
    val sharedPreferences: SharedPreferences,
    val walletDao: WalletDao,
    val gdk: Gdk,
    val settingsManager: SettingsManager,
    val database: Database
) {

    val keyStore: KeyStore by lazy {
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
    }

    fun migrate(){
        if (sharedPreferences.getLong(Preferences.APP_DATA_VERSION, 0) < APP_DATA_VERSION) {
            runBlocking {
                MigratorJava.migratePreferencesFromV2(context)

                migratePreferencesFromV3()
                fixV4Migration()

                migrateAppDataV2()
                migrateAppDataV3()
            }
        }
    }

    private suspend fun migratePreferencesFromV3(){
        if(sharedPreferences.getBoolean(Preferences.MIGRATED_V3_V4_1, false) || sharedPreferences.getBoolean(Preferences.MIGRATED_V3_V4_2, false)){
            return
        }

        var enableTOR = false
        var proxyURL : String? = null

        val networks = listOf("mainnet", "liquid", "testnet")

        for(networkId in networks){
            val networkPreferences = context.getSharedPreferences(networkId, Context.MODE_PRIVATE)

            val pinPreferences = listOf("pin", "pin_sec").map {
                context.getSharedPreferences(
                    "${networkId}_$it",
                    Context.MODE_PRIVATE
                )
            }

            // Check if wallet exists
            if(!pinPreferences.any { it.contains("ident") }){
                continue
            }

            // Update Proxy settings only if are enabled. Keep only the first value
            if(networkPreferences.getBoolean(PROXY_ENABLED, false) && proxyURL == null){
                proxyURL = networkPreferences.getString(PROXY_HOST, "") + ":" + networkPreferences.getString(
                    PROXY_PORT,
                    ""
                )
            }

            enableTOR = networkPreferences.getBoolean(TOR_ENABLED, false) || enableTOR

            val network = gdk.networks().getNetworkById(networkId)

            val wallet = Wallet(
                walletHashId = "",
                name = network.name,
                activeNetwork = network.id,
                isRecoveryPhraseConfirmed = true,
                isHardware = false,
                isTestnet = network.isTestnet,
                activeAccount = networkPreferences.getInt(ACTIVE_SUBACCOUNT, 0).toLong()
            )

            wallet.id = walletDao.insertWallet(wallet)

            for(pinPreference in pinPreferences){
                if(pinPreference.contains("ident")){

                    val pinData = fromV3PreferenceValues(pinPreference)

                    // Beware: can be empty
                    val nativePIN = pinPreference.getString("native", null)
                    val nativeIV = pinPreference.getString("nativeiv", null)

                    val credentialType: CredentialType
                    var keystore : String? = null
                    var encryptedData: EncryptedData? = null

                    if(nativePIN.isNullOrBlank()){
                        // User PIN
                        credentialType = if(pinPreference.getBoolean("is_six_digit", false)) CredentialType.PIN_PINDATA else CredentialType.PASSWORD_PINDATA
                    }else{
                        // Biometrics or Screenlock
                        credentialType = CredentialType.BIOMETRICS_PINDATA
                        keystore = getV3KeyName(networkId)

                        encryptedData = EncryptedData(nativePIN, nativeIV ?: "")
                    }

                    walletDao.insertOrReplaceLoginCredentials(
                        LoginCredentials(
                            walletId = wallet.id,
                            network = network.id,
                            credentialType = credentialType,
                            pinData = pinData,
                            keystore = keystore,
                            encryptedData = encryptedData,
                            counter = pinPreference.getInt("counter", 0).toLong()
                        )
                    )
                }
            }
        }

        val appSettings = settingsManager.getApplicationSettings()
        settingsManager.saveApplicationSettings(
            appSettings.copy(
                tor = enableTOR,
                proxyUrl = proxyURL
            )
        )

        sharedPreferences.edit().putBoolean(Preferences.MIGRATED_V3_V4_2, true).apply()
    }

    private suspend fun fixV4Migration(){
        if(sharedPreferences.getBoolean(Preferences.MIGRATED_V3_V4_2, false)){
            return
        }

        val networks = listOf("mainnet", "liquid", "testnet")

        for(networkId in networks){
            val pinPreferences = listOf("pin", "pin_sec").map {
                context.getSharedPreferences(
                    "${networkId}_$it",
                    Context.MODE_PRIVATE
                )
            }

            // Check if wallet exists
            if(!pinPreferences.any { it.contains("ident") }){
                continue
            }

            val batchUpdate = mutableListOf<LoginCredentials>()
            val batchDelete = mutableListOf<LoginCredentials>()

            for(pinPreference in pinPreferences){
                if(pinPreference.contains("ident")){
                    val pinData = fromV3PreferenceValues(pinPreference)

                    // Beware: can be empty
                    val nativePIN = pinPreference.getString("native", null)
                    val nativeIV = pinPreference.getString("nativeiv", null)

                    val credentialType: CredentialType
                    var keystore : String? = null
                    var encryptedData: EncryptedData? = null

                    if(nativePIN.isNullOrBlank()){
                        // User Pin
                        credentialType = if(pinPreference.getBoolean("is_six_digit", false)) CredentialType.PIN_PINDATA else CredentialType.PASSWORD_PINDATA
                    }else{
                        // Biometrics or Screenlock
                        credentialType = CredentialType.BIOMETRICS_PINDATA
                        keystore = getV3KeyName(networkId)
                        encryptedData = EncryptedData(nativePIN, nativeIV ?: "")
                    }

                    for(wallet in walletDao.getAllWallets()){
                        for (loginCredentials in walletDao.getLoginCredentialsSuspend(wallet.id)){

                            if(pinData == loginCredentials.pinData){
                                batchDelete += loginCredentials.copy()

                                // update from SharedPreferences
                                loginCredentials.credentialType = credentialType
                                loginCredentials.keystore = keystore
                                loginCredentials.encryptedData = encryptedData

                                batchUpdate += loginCredentials
                            }
                        }
                    }
                }
            }

            // As we change the primary key, we have to first delete the old record
            for(deleteLoginCredentials in batchDelete){
                walletDao.deleteLoginCredentials(deleteLoginCredentials)
            }

            // and insert the new login credentials
            for(inserLoginCredentials in batchUpdate){
                walletDao.insertOrReplaceLoginCredentials(inserLoginCredentials)
            }
        }

        sharedPreferences.edit().putBoolean(Preferences.MIGRATED_V3_V4_2, true).apply()
    }

    private suspend fun migrateAppDataV2() {
        if (sharedPreferences.getLong(Preferences.APP_DATA_VERSION, 0) < 2) {

            logger.i { "Migrating AppData to v2" }

            walletDao.getAllWallets().forEach { wallet ->

                val network = wallet.activeNetwork

                wallet.isTestnet = wallet.activeNetwork.contains("testnet")

                walletDao.updateWallet(wallet)

                walletDao.getLoginCredentialsSuspend(wallet.id).forEach {
                    it.network = network
                    walletDao.updateLoginCredentials(it)
                }
            }

            sharedPreferences.edit().putLong(Preferences.APP_DATA_VERSION, 2).apply()
        }
    }

    private suspend fun migrateAppDataV3() {
        if (sharedPreferences.getLong(Preferences.APP_DATA_VERSION, 0) < 3) {
            logger.i { "Migrating AppData to v3" }

            walletDao.getAllWallets().forEach { wallet ->
                val newWallet = roomToDelight(wallet).toGreenWallet().also {
                    database.insertWallet(it)
                }

                walletDao.getLoginCredentialsSuspend(wallet.id).forEach { loginCredentials ->
                    database.replaceLoginCredential(roomToDelight(newWallet, loginCredentials))
                }
            }

            walletDao.getSoftwareWallets().lastOrNull()?.also {
                settingsManager.increaseWalletCounter(it.id.toInt())
            }

            sharedPreferences.edit().putLong(Preferences.APP_DATA_VERSION, 3).apply()
        }
    }

    private fun fromV3PreferenceValues(pin: SharedPreferences): PinData? {
        try {
            val pinIdentifier = pin.getString("ident", null)!!

            val split = pin.getString("encrypted", null)!!.split(";".toRegex()).toTypedArray()
            val salt = split[0]

            val encryptedDataBytes = Base64.decode(split[1], Base64.NO_WRAP)
            val encryptedData = encryptedDataBytes.toHex()

            return PinData(encryptedData = encryptedData, pinIdentifier = pinIdentifier, salt = salt)
        }catch (e: Exception){
            e.printStackTrace()
        }
        return null
    }

    private fun getV3KeyName(network: String): String {
        // Check if the key is in the Keystore
        val key = "NativeAndroidAuth_$network"
        try {
            if(keyStore.getKey(key, null) != null){
                return key
            }
        }catch (e: UnrecoverableKeyException){
            // Key is invalidated, let LoginFragment handle it
            e.printStackTrace()
            return key
        }

        // Else is from v2
        return "NativeAndroidAuth"
    }

    companion object: Loggable(){
        const val APP_DATA_VERSION = 3
        const val NETWORK_ID_ACTIVE = "network_id_active"
        const val VERSION = "version"
        const val PROXY_ENABLED = "proxy_enabled"
        const val PROXY_HOST = "proxy_host"
        const val PROXY_PORT = "proxy_port"
        const val TOR_ENABLED = "tor_enabled"
        const val TRUSTED_ADDRESS = "trusted_address"
        const val SPV_ENABLED = "spv_enabled"
        const val ACTIVE_SUBACCOUNT = "active_subaccount"
    }
}