package com.blockstream.green.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.blockstream.green.utils.SecureRandom
import com.blockstream.green.utils.isDevelopmentFlavor
import java.util.*

class SettingsManager constructor(context: Context, private val sharedPreferences: SharedPreferences) {

    private var appSettingsSharedPreferences: SharedPreferences =
        context.getSharedPreferences(APPLICATION_SETTINGS_NAME, Context.MODE_PRIVATE)

    private var appSettings = MutableLiveData(ApplicationSettings.fromSharedPreferences(context.isDevelopmentFlavor(), appSettingsSharedPreferences))

    fun getApplicationSettingsLiveData() = appSettings

    fun getApplicationSettings() = appSettings.value!!

    fun saveApplicationSettings(newAppSettings: ApplicationSettings){
        appSettings.postValue(newAppSettings)
        ApplicationSettings.toSharedPreferences(newAppSettings, appSettingsSharedPreferences)
    }

    fun isDeviceTermsAccepted() = sharedPreferences.getInt(KEY_DEVICE_TERMS_ACCEPTED, 0) == 1
    fun setDeviceTermsAccepted() = sharedPreferences.edit().putInt(KEY_DEVICE_TERMS_ACCEPTED, 1).apply()

    fun isAskedAboutAnalyticsConsent() = sharedPreferences.getInt(KEY_ASKED_ANALYTICS_CONSENT, 0) == 1
    fun setAskedAboutAnalyticsConsent() = sharedPreferences.edit().putInt(KEY_ASKED_ANALYTICS_CONSENT, 1).apply()

    fun getCountlyDeviceId(): String {
        return sharedPreferences.getString(KEY_COUNTLY_DEVICE_ID, null) ?: run {
            UUID.randomUUID().toString().also {
                sharedPreferences.edit().putString(KEY_COUNTLY_DEVICE_ID, it).apply()
            }
        }
    }

    fun resetCountlyDeviceId(){
        sharedPreferences.edit().remove(KEY_COUNTLY_DEVICE_ID).apply()
    }

    fun getCountlyOffset(end: Long): Long {
        return sharedPreferences.getLong(KEY_COUNTLY_OFFSET, -1).takeIf { it >= 0 } ?: run {
            (0 .. end).random(SecureRandom).also {
                sharedPreferences.edit().putLong(KEY_COUNTLY_OFFSET, it).apply()
            }
        }
    }

    fun resetCountlyOffset(){
        sharedPreferences.edit().remove(KEY_COUNTLY_OFFSET).apply()
    }

    fun zeroCountlyOffset(){
        sharedPreferences.edit().putLong(KEY_COUNTLY_OFFSET, 0).apply()
    }

    companion object {
        const val APPLICATION_SETTINGS_NAME = "application_settings"

        const val KEY_DEVICE_TERMS_ACCEPTED = "device_terms_accepted"
        const val KEY_ASKED_ANALYTICS_CONSENT = "asked_analytics_consent"
        const val KEY_COUNTLY_DEVICE_ID = "countly_device_id"
        const val KEY_COUNTLY_OFFSET = "countly_offset"
    }
}