package com.blockstream.green.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.preference.PreferenceManager
import com.blockstream.gdk.GreenWallet
import com.blockstream.gdk.data.PinData
import com.blockstream.green.EncryptedData
import com.blockstream.green.Preferences
import com.blockstream.green.database.CredentialType
import com.blockstream.green.database.LoginCredentials
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.libwally.Wally
import com.greenaddress.greenbits.ui.preferences.PrefKeys

class Migrator(
    val context: Context,
    val walletRepository: WalletRepository,
    val greenWallet: GreenWallet,
    val settingsManager: SettingsManager,
) {

    fun migrate(){

        MigratorJava.migratePreferencesFromV2(context)

        migratePreferencesFromV3()
    }

    private fun migratePreferencesFromV3(){
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        if(sharedPreferences.getBoolean(Preferences.MIGRATED_V3_V4, false)){
            return
        }

        var enableSPV = false
        var enableTOR = false
        var proxyURL : String? = null

        val networks = listOf("mainnet", "liquid", "testnet")

        for(networkId in networks){
            val networkPreferences = context.getSharedPreferences(networkId, Context.MODE_PRIVATE)

            if(!networkPreferences.contains(PrefKeys.DEVICE_ID)){
                continue
            }

            val pinPreferences = context.getSharedPreferences(
                "${networkId}_pin",
                Context.MODE_PRIVATE
            )
            val biometricsPreferences = context.getSharedPreferences(
                "${networkId}_pin_sec",
                Context.MODE_PRIVATE
            )

            // Migrate SPV, if any network has enabled SPV, keep it on for all
            // TODO migrate peers


            // Update Proxy settings only if are enabled. Keep only the first value
            if(networkPreferences.getBoolean(PrefKeys.PROXY_ENABLED, false) && proxyURL == null){
                proxyURL = networkPreferences.getString(PrefKeys.PROXY_HOST, "") + ":" + networkPreferences.getString(
                    PrefKeys.PROXY_PORT,
                    ""
                )
            }

            enableSPV = networkPreferences.getBoolean(PrefKeys.SPV_ENABLED, enableSPV)
            enableTOR = networkPreferences.getBoolean(PrefKeys.TOR_ENABLED, enableTOR)

            val network = greenWallet.networks.getNetworkById(networkId)

            val wallet = Wallet(
                name = network.name,
                network = network.id,
                isElectrum = network.isElectrum,
                isRecoveryPhraseConfirmed = true,
                isHardware = false,
                activeAccount = networkPreferences.getInt(PrefKeys.ACTIVE_SUBACCOUNT, 0).toLong()
            )

            wallet.id = walletRepository.addWallet(wallet)

            // Migrate PinData
            if(pinPreferences.contains("encrypted")){
                val pinData = fromV3PreferenceValues(pinPreferences)

                // Important: is_six_digit flag was added in v3
                val credentialType = if(pinPreferences.getBoolean("is_six_digit", false)) CredentialType.PIN else CredentialType.PASSWORD

                walletRepository.addLoginCredentials(
                    LoginCredentials(
                        walletId = wallet.id,
                        credentialType = credentialType,
                        pinData = pinData,
                        counter = pinPreferences.getInt("counter", 0),
                    )
                )
            }

            // Migrate Biometrics Data
            if(biometricsPreferences.contains("encrypted")){
                val pinData = fromV3PreferenceValues(biometricsPreferences)

                val nativePIN = biometricsPreferences.getString("native", null)
                val nativeIV = biometricsPreferences.getString("nativeiv", null)

                val encryptedData = EncryptedData(nativePIN ?: "", nativeIV ?: "")

                walletRepository.addLoginCredentials(
                    LoginCredentials(
                        walletId = wallet.id,
                        credentialType = CredentialType.BIOMETRICS,
                        pinData = pinData,
                        keystore = getV3KeyName(networkId),
                        encryptedData = encryptedData,
                        counter = biometricsPreferences.getInt("counter", 0)
                    )
                )
            }
        }

        val appSettings = settingsManager.getApplicationSettings()
        settingsManager.saveApplicationSettings(
            appSettings.copy(
                spv = enableSPV,
                tor = enableTOR,
                proxyURL = proxyURL
            )
        )

        sharedPreferences.edit().putBoolean(Preferences.MIGRATED_V3_V4, true).apply()
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
        return "NativeAndroidAuth_$network"
    }
}