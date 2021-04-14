package com.blockstream.green.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Parcelable
import android.widget.Toast
import kotlinx.parcelize.Parcelize


@Parcelize
data class ApplicationSettings(
    val proxyURL: String? = null,
    val tor: Boolean = false,
) : Parcelable {

    fun trace(context: Context) {
        Toast.makeText(
            context,
            "proxyURL: $proxyURL\n" +
                    "enabledTor: $tor\n",
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {
        private const val PROXY_URL = "proxyURL"
        private const val TOR = "tor"

        fun fromSharedPreferences(prefs: SharedPreferences): ApplicationSettings {
            return ApplicationSettings(
                proxyURL = prefs.getString(PROXY_URL , null),
                tor = prefs.getBoolean(TOR , false),
            )
        }

        fun toSharedPreferences(appSettings: ApplicationSettings, prefs: SharedPreferences) {
            prefs.edit().also {
                it.putString(PROXY_URL, appSettings.proxyURL)
                it.putBoolean(TOR , appSettings.tor)
            }.apply()
        }
    }
}