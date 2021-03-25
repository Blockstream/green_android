package com.blockstream.green.settings

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    private var sharedPreferences: SharedPreferences =
        context.getSharedPreferences(APPLICATION_SETTINGS_NAME, Context.MODE_PRIVATE)

    private var appSettings = ApplicationSettings.fromSharedPreferences(sharedPreferences)


    fun getApplicationSettings() = appSettings

    fun saveApplicationSettings(newAppSettings: ApplicationSettings){
        appSettings = newAppSettings
        ApplicationSettings.toSharedPreferences(appSettings, sharedPreferences)
    }

    companion object {
        const val APPLICATION_SETTINGS_NAME = "application_settings"
    }
}