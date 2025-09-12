@file:OptIn(ExperimentalUuidApi::class)

package com.blockstream.common.managers

import com.blockstream.common.data.ApplicationSettings
import com.blockstream.common.utils.server
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesIgnore
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import saschpe.kase64.base64UrlEncoded
import kotlin.random.Random
import kotlin.random.nextLong
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SettingsManager constructor(
    private val settings: ObservableSettings,
    val analyticsFeatureEnabled: Boolean,
    val lightningFeatureEnabled: Boolean,
    val storeRateEnabled: Boolean
) {
    private var _appSettings = MutableStateFlow(ApplicationSettings.fromSettings(settings))

    val appSettings
        get() = _appSettings.value

    @NativeCoroutinesIgnore
    val appSettingsStateFlow
        get() = _appSettings.asStateFlow()

    fun getApplicationSettings() = appSettings

    fun saveApplicationSettings(newAppSettings: ApplicationSettings) {
        _appSettings.value = newAppSettings
        ApplicationSettings.saveSettings(newAppSettings, settings)
    }

    fun isDeviceTermsAccepted() = settings[KEY_DEVICE_TERMS_ACCEPTED, 0] == 1
    fun setDeviceTermsAccepted() = settings.set(KEY_DEVICE_TERMS_ACCEPTED, 1)

    private fun keyForCustomPinServer(urls: List<String>): String {
        return urls.joinToString("_") {
            it.server()
        }.base64UrlEncoded.let {
            "${KEY_ALLOW_CUSTOM_PIN_SERVER}_$it"
        }
    }

    fun isAllowCustomPinServer(url: List<String>) = settings[keyForCustomPinServer(url), false]

    fun setAllowCustomPinServer(url: List<String>) {
        settings[keyForCustomPinServer(url)] = true
    }

    private fun keyForPromo(id: String): String {
        return "${KEY_PROMO_DISMISSED}_$id"
    }

    fun isPromoDismissed(id: String) = settings[keyForPromo(id), false]
    fun dismissPromo(id: String) {
        settings[keyForPromo(id)] = true
    }

    fun resetPromoDismissals() {
        settings.keys.filter { it.startsWith(KEY_PROMO_DISMISSED) }.forEach {
            settings.remove(it)
        }
    }

    fun isAskedAboutAnalyticsConsent() = settings[KEY_ASKED_ANALYTICS_CONSENT, 0] == 1

    fun setAskedAboutAnalyticsConsent() = settings.set(KEY_ASKED_ANALYTICS_CONSENT, 1)

    fun whenIsAskedAboutAppReview(): Instant =
        Instant.fromEpochMilliseconds(settings[KEY_ASKED_APP_REVIEW, 0L])

    fun setAskedAboutAppReview() {
        // Set now in milliseconds
        settings[KEY_ASKED_APP_REVIEW] = Clock.System.now().toEpochMilliseconds()
    }

    fun isV5Upgraded(): Boolean = settings.getBoolean(KEY_V5_UPGRADE, false)

    @OptIn(ExperimentalSettingsApi::class)
    fun isV5UpgradedFlow() = settings.getBooleanFlow(KEY_V5_UPGRADE, false)

    fun setV5Upgraded() {
        settings[KEY_V5_UPGRADE] = true
    }

    fun getCountlyDeviceId(): String {
        return settings[KEY_COUNTLY_DEVICE_ID] ?: run {
            Uuid.random().toString().also {
                settings.putString(KEY_COUNTLY_DEVICE_ID, it)
            }
        }
    }

    fun resetCountlyDeviceId() {
        settings.remove(KEY_COUNTLY_DEVICE_ID)
    }

    fun getCountlyOffset(end: Long): Long {
        return settings[KEY_COUNTLY_OFFSET, -1L].takeIf { it >= 0 } ?: Random.nextLong(0L..end).also {
            settings.putLong(KEY_COUNTLY_OFFSET, it)
        }
    }

    fun resetCountlyOffset() {
        settings.remove(KEY_COUNTLY_OFFSET)
    }

    fun zeroCountlyOffset() {
        settings.putLong(KEY_COUNTLY_OFFSET, 0L)
    }

    fun isLightningEnabled(): Boolean {
        return lightningFeatureEnabled && appSettings.experimentalFeatures
    }

    fun walletCounter(): Int {
        return settings[KEY_WALLET_COUNTER, 0]
    }

    fun increaseWalletCounter(force: Int? = null) {
        settings[KEY_WALLET_COUNTER] = force ?: (walletCounter() + 1)
    }

    fun getCountry(): String? = settings[KEY_COUNTRY]

    fun setCountry(country: String) {
        settings[KEY_COUNTRY] = country
    }

    fun clearAll() {
        settings.clear()
    }

    companion object {
        const val APPLICATION_SETTINGS_NAME = "application_settings"

        const val KEY_DEVICE_TERMS_ACCEPTED = "device_terms_accepted"
        const val KEY_ASKED_ANALYTICS_CONSENT = "asked_analytics_consent"
        const val KEY_ASKED_APP_REVIEW = "asked_app_review"
        const val KEY_COUNTLY_DEVICE_ID = "countly_device_id"
        const val KEY_COUNTLY_OFFSET = "countly_offset"
        const val KEY_ALLOW_CUSTOM_PIN_SERVER = "allow_custom_pin_server"
        const val KEY_PROMO_DISMISSED = "promo_dismissed"
        const val KEY_WALLET_COUNTER = "wallet_counter"
        const val KEY_COUNTRY = "country"
        const val KEY_V5_UPGRADE = "v5_upgrade"
    }
}