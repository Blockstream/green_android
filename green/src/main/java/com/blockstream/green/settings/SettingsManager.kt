package com.blockstream.green.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager

class SettingsManager(context: Context) {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private var appSettingsSharedPreferences: SharedPreferences =
        context.getSharedPreferences(APPLICATION_SETTINGS_NAME, Context.MODE_PRIVATE)

    private var appSettings = MutableLiveData(ApplicationSettings.fromSharedPreferences(appSettingsSharedPreferences))

    fun getApplicationSettingsLiveData() = appSettings

    fun getApplicationSettings() = appSettings.value!!

    fun saveApplicationSettings(newAppSettings: ApplicationSettings){
        appSettings.value = newAppSettings
        ApplicationSettings.toSharedPreferences(newAppSettings, appSettingsSharedPreferences)
    }

    fun showTorSinglesigWarning(): Boolean{
        return !sharedPreferences.getBoolean(KEY_TOR_WARNING, false)
    }

    fun setTorSinglesigWarned() = sharedPreferences.edit().putBoolean(KEY_TOR_WARNING, true).apply()

    companion object {
        const val APPLICATION_SETTINGS_NAME = "application_settings"

        const val KEY_TOR_WARNING = "tor_singlesig_warned"
    }
}