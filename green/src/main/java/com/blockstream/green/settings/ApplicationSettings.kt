package com.blockstream.green.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Parcelable
import android.widget.Toast
import kotlinx.parcelize.Parcelize


@Parcelize
data class ApplicationSettings(
    val proxyURL: String? = null,
    val bitcoinElectrumBackendURL: String? = null,
    val liquidElectrumBackendURL: String? = null,

    val tor: Boolean = false,
    val multiServerValidation: Boolean = false,
    val spv: Boolean = false,
) : Parcelable {

    fun trace(context: Context) {
        Toast.makeText(
            context,
            "proxyURL: $proxyURL\n" +
                    "customElectrumBackendURL: $bitcoinElectrumBackendURL\n" +
                    "liquidElectrumBackendURL: $liquidElectrumBackendURL\n" +
                    "enabledTor: $tor\n" +
                    "enabledMultiServerValidation: $multiServerValidation\n" +
                    "SPV: $spv",
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {

        private const val PROXY_URL = "proxyURL"
        private const val BITCOIN_ELECTRUM_BACKEND_URL = "bitcoinElectrumBackendURL"
        private const val LIQUID_ELECTRUM_BACKEND_URL = "liquidElectrumBackendURL"
        private const val TOR = "tor"
        private const val MULTI_SERVER_VALIDATION = "multiServerValidation"
        private const val SPV = "spv"

        fun fromSharedPreferences(prefs: SharedPreferences): ApplicationSettings {
            return ApplicationSettings(
                proxyURL = prefs.getString(PROXY_URL , null),
                bitcoinElectrumBackendURL = prefs.getString(BITCOIN_ELECTRUM_BACKEND_URL , null),
                liquidElectrumBackendURL = prefs.getString(LIQUID_ELECTRUM_BACKEND_URL , null),
                tor = prefs.getBoolean(TOR , false),
                multiServerValidation = prefs.getBoolean(MULTI_SERVER_VALIDATION , false),
                spv = prefs.getBoolean(SPV , false)
            )
        }

        fun toSharedPreferences(appSettings: ApplicationSettings, prefs: SharedPreferences) {
            val editor = prefs.edit()
            editor.putString(PROXY_URL, appSettings.proxyURL)
            editor.putString(BITCOIN_ELECTRUM_BACKEND_URL, appSettings.bitcoinElectrumBackendURL)
            editor.putString(LIQUID_ELECTRUM_BACKEND_URL, appSettings.liquidElectrumBackendURL)

            editor.putBoolean(TOR , appSettings.tor)
            editor.putBoolean(MULTI_SERVER_VALIDATION , appSettings.multiServerValidation)
            editor.putBoolean(SPV , appSettings.spv)

            editor.apply()
        }
    }

}