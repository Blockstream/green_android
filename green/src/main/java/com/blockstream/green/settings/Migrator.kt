package com.blockstream.green.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.preference.PreferenceManager
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.data.PinData
import com.blockstream.green.ApplicationScope
import com.blockstream.green.Preferences
import com.blockstream.green.database.CredentialType
import com.blockstream.green.database.LoginCredentials
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.utils.EncryptedData
import com.blockstream.libwally.Wally
import mu.KLogging
import java.security.KeyStore
import java.security.UnrecoverableKeyException

class Migrator constructor(
    val context: Context,
    val walletRepository: WalletRepository,
    val greenWallet: GreenWallet,
    val settingsManager: SettingsManager,
    val applicationScope: ApplicationScope
) {

    var keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    suspend fun migrate(){
        MigratorJava.migratePreferencesFromV2(context)

        migratePreferencesFromV3()
        fixV4Migration()
    }

    private suspend fun migratePreferencesFromV3(){
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

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

            val network = greenWallet.networks.getNetworkById(networkId)

            val wallet = Wallet(
                walletHashId = "",
                name = network.name,
                network = network.id,
                isRecoveryPhraseConfirmed = true,
                isHardware = false,
                activeAccount = networkPreferences.getInt(ACTIVE_SUBACCOUNT, 0).toLong()
            )

            wallet.id = walletRepository.addWalletSuspend(wallet)

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
                        credentialType = if(pinPreference.getBoolean("is_six_digit", false)) CredentialType.PIN else CredentialType.PASSWORD
                    }else{
                        // Biometrics or Screenlock
                        credentialType = CredentialType.BIOMETRICS
                        keystore = getV3KeyName(networkId)

                        encryptedData = EncryptedData(nativePIN, nativeIV ?: "")
                    }

                    walletRepository.addLoginCredentialsSuspend(
                        LoginCredentials(
                            walletId = wallet.id,
                            credentialType = credentialType,
                            pinData = pinData,
                            keystore = keystore,
                            encryptedData = encryptedData,
                            counter = pinPreference.getInt("counter", 0)
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
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

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
                        credentialType = if(pinPreference.getBoolean("is_six_digit", false)) CredentialType.PIN else CredentialType.PASSWORD
                    }else{
                        // Biometrics or Screenlock
                        credentialType = CredentialType.BIOMETRICS
                        keystore = getV3KeyName(networkId)
                        encryptedData = EncryptedData(nativePIN, nativeIV ?: "")
                    }

                    for(wallet in walletRepository.getWalletsSuspend()){
                        for (loginCredentials in walletRepository.getLoginCredentialsSuspend(wallet.id)){

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
                walletRepository.deleteLoginCredentialsSuspend(deleteLoginCredentials)
            }

            // and insert the new login credentials
            for(inserLoginCredentials in batchUpdate){
                walletRepository.addLoginCredentialsSuspend(inserLoginCredentials)
            }
        }

        sharedPreferences.edit().putBoolean(Preferences.MIGRATED_V3_V4_2, true).apply()
    }

    private fun fromV3PreferenceValues(pin: SharedPreferences): PinData? {
        try {
            val pinIdentifier = pin.getString("ident", null)!!

            val split = pin.getString("encrypted", null)!!.split(";".toRegex()).toTypedArray()
            val salt = split[0]

            val encryptedDataBytes = Base64.decode(split[1], Base64.NO_WRAP)
            val encryptedData = Wally.hex_from_bytes(encryptedDataBytes)

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

    companion object: KLogging(){
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