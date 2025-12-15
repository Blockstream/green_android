package com.blockstream.compose.models.settings

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_app_settings
import com.blockstream.data.data.ApplicationSettings
import com.blockstream.data.data.ScreenLockSetting
import com.blockstream.data.extensions.isNotBlank
import com.blockstream.data.gdk.events.GenericEvent
import com.blockstream.data.managers.LocaleManager
import com.blockstream.data.managers.Locales
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffect
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.utils.Loggable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

abstract class AppSettingsViewModelAbstract() :
    GreenViewModel() {
    override fun screenName(): String = "AppSettings"

    abstract val multiServerValidationFeatureEnabled: Boolean

    abstract val analyticsFeatureEnabled: Boolean

    abstract val experimentalFeatureEnabled: Boolean
    abstract val enhancedPrivacyEnabled: MutableStateFlow<Boolean>
    abstract val screenLockInSeconds: MutableStateFlow<ScreenLockSetting>
    abstract val torEnabled: MutableStateFlow<Boolean>
    abstract val proxyEnabled: MutableStateFlow<Boolean>
    abstract val proxyUrl: MutableStateFlow<String>
    abstract val rememberHardwareDevices: MutableStateFlow<Boolean>
    abstract val testnetEnabled: MutableStateFlow<Boolean>
    abstract val experimentalFeaturesEnabled: MutableStateFlow<Boolean>
    abstract val analyticsEnabled: MutableStateFlow<Boolean>
    abstract val electrumNodeEnabled: MutableStateFlow<Boolean>
    abstract val personalElectrumServerTlsEnabled: MutableStateFlow<Boolean>
    abstract val personalBitcoinElectrumServer: MutableStateFlow<String>
    abstract val personalLiquidElectrumServer: MutableStateFlow<String>
    abstract val personalTestnetElectrumServer: MutableStateFlow<String>
    abstract val personalTestnetLiquidElectrumServer: MutableStateFlow<String>
    abstract val electrumServerGapLimit: MutableStateFlow<String>
    abstract val locales: MutableStateFlow<Map<String?, String?>>
    abstract val locale: MutableStateFlow<String?>

    companion object {
        const val DEFAULT_BITCOIN_ELECTRUM_URL = "blockstream.info:700"
        const val DEFAULT_LIQUID_ELECTRUM_URL = "blockstream.info:995"
        const val DEFAULT_TESTNET_ELECTRUM_URL = "blockstream.info:993"
        const val DEFAULT_TESTNET_LIQUID_ELECTRUM_URL = "blockstream.info:465"

        const val DEFAULT_MULTI_SPV_BITCOIN_URL = "electrum.blockstream.info:50002"
        const val DEFAULT_MULTI_SPV_LIQUID_URL = "blockstream.info:995"
        const val DEFAULT_MULTI_SPV_TESTNET_URL = "electrum.blockstream.info:60002"
        const val DEFAULT_MULTI_SPV_TESTNET_LIQUID_URL = "blockstream.info:465"
    }
}

class AppSettingsViewModel : AppSettingsViewModelAbstract() {
    private val localeManager: LocaleManager by inject()

    private var appSettings: ApplicationSettings = settingsManager.getApplicationSettings()

    override val multiServerValidationFeatureEnabled = appInfo.isDevelopmentOrDebug

    override val analyticsFeatureEnabled = settingsManager.analyticsFeatureEnabled

    override val experimentalFeatureEnabled = settingsManager.lightningFeatureEnabled
    override val enhancedPrivacyEnabled = MutableStateFlow(appSettings.enhancedPrivacy)
    override val screenLockInSeconds = MutableStateFlow(ScreenLockSetting.bySeconds(appSettings.screenLockInSeconds))
    override val torEnabled = MutableStateFlow(appSettings.tor)
    override val proxyEnabled = MutableStateFlow(appSettings.proxyUrl.isNotBlank())
    override val proxyUrl = MutableStateFlow(appSettings.proxyUrl ?: "")
    override val rememberHardwareDevices = MutableStateFlow(appSettings.rememberHardwareDevices)
    override val testnetEnabled = MutableStateFlow(appSettings.testnet)
    override val experimentalFeaturesEnabled = MutableStateFlow(appSettings.experimentalFeatures)
    override val analyticsEnabled = MutableStateFlow(appSettings.analytics)
    override val electrumNodeEnabled = MutableStateFlow(appSettings.electrumNode)

    override val personalElectrumServerTlsEnabled: MutableStateFlow<Boolean> =
        MutableStateFlow(appSettings.personalElectrumServerTls)
    override val personalBitcoinElectrumServer: MutableStateFlow<String> =
        MutableStateFlow(appSettings.personalBitcoinElectrumServer ?: "")
    override val personalLiquidElectrumServer: MutableStateFlow<String> =
        MutableStateFlow(appSettings.personalLiquidElectrumServer ?: "")
    override val personalTestnetElectrumServer: MutableStateFlow<String> =
        MutableStateFlow(appSettings.personalTestnetElectrumServer ?: "")
    override val personalTestnetLiquidElectrumServer: MutableStateFlow<String> =
        MutableStateFlow(appSettings.personalTestnetLiquidElectrumServer ?: "")

    override val electrumServerGapLimit: MutableStateFlow<String> =
        MutableStateFlow("${appSettings.electrumServerGapLimit ?: "20"}")
    override val locales = MutableStateFlow(Locales)
    override val locale: MutableStateFlow<String?> = MutableStateFlow(localeManager.getLocale())

    class LocalEvents {
        object AnalyticsMoreInfo : Events.EventSideEffect(SideEffects.NavigateTo(NavigateDestinations.Analytics()))
        object Save : Event
        object AutoSave : Event
        object Cancel : Event
    }

    class LocalSideEffects {
        object UnsavedAppSettings : SideEffect
    }

    init {

        viewModelScope.launch {
            _navData.value = NavData(title = getString(Res.string.id_app_settings))
            database.insertEvent(GenericEvent(deviceId = settingsManager.getCountlyDeviceId()).sha256(), randomInsert = true)
        }

        combine(
            enhancedPrivacyEnabled,
            screenLockInSeconds,
            testnetEnabled,
            analyticsEnabled,
            experimentalFeaturesEnabled,
            locale,
            proxyUrl,
            rememberHardwareDevices,
            electrumNodeEnabled,
            torEnabled,
            electrumServerGapLimit,
            personalBitcoinElectrumServer,
            personalLiquidElectrumServer,
            personalTestnetElectrumServer,
            personalTestnetLiquidElectrumServer,
            personalElectrumServerTlsEnabled
        ) {
            _navData.value = _navData.value.copy(backHandlerEnabled = areSettingsDirty())
        }.launchIn(viewModelScope)

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.Save -> {
                localeManager.setLocale(locale.value)
                settingsManager.saveApplicationSettings(getSettings())
                postSideEffect(SideEffects.NavigateBack())
            }

            is LocalEvents.AutoSave -> {
                localeManager.setLocale(locale.value)
                settingsManager.saveApplicationSettings(getSettings())
            }

            is LocalEvents.Cancel -> {
                postSideEffect(SideEffects.NavigateBack())
            }

            is Events.NavigateBackUserAction -> {
                if (areSettingsDirty()) {
                    postSideEffect(LocalSideEffects.UnsavedAppSettings)
                } else {
                    postSideEffect(SideEffects.NavigateBack())
                }
            }
        }
    }

    private fun getSettings() = ApplicationSettings(
        enhancedPrivacy = enhancedPrivacyEnabled.value,
        screenLockInSeconds = screenLockInSeconds.value.seconds,
        testnet = testnetEnabled.value,
        analytics = analyticsEnabled.value,
        experimentalFeatures = experimentalFeaturesEnabled.value,
        locale = locale.value,
        proxyUrl = proxyUrl.value.takeIf { it.isNotBlank() && proxyEnabled.value },
        rememberHardwareDevices = rememberHardwareDevices.value,
        electrumNode = electrumNodeEnabled.value,
        tor = torEnabled.value,
        electrumServerGapLimit = electrumServerGapLimit.value.takeIf { it.isNotBlank() && it != "20" }?.toIntOrNull(),

        // use null value as a reset to re-set the default urls and blank as a way to disabled it for a specific network
        personalBitcoinElectrumServer = personalBitcoinElectrumServer.value.takeIf { electrumNodeEnabled.value },
        personalLiquidElectrumServer = personalLiquidElectrumServer.value.takeIf { electrumNodeEnabled.value },
        personalTestnetElectrumServer = personalTestnetElectrumServer.value.takeIf { electrumNodeEnabled.value },
        personalTestnetLiquidElectrumServer = personalTestnetLiquidElectrumServer.value.takeIf { electrumNodeEnabled.value },

        personalElectrumServerTls = personalElectrumServerTlsEnabled.value,
    )

    private fun areSettingsDirty(): Boolean {
        return getSettings() != appSettings
    }

    companion object : Loggable()
}

class AppSettingsViewModelPreview(initValue: Boolean = false) : AppSettingsViewModelAbstract() {

    companion object {
        fun preview(initValue: Boolean) = AppSettingsViewModelPreview(initValue)
    }

    override val multiServerValidationFeatureEnabled: Boolean = false
    override val analyticsFeatureEnabled: Boolean = true
    override val experimentalFeatureEnabled: Boolean = true
    override val enhancedPrivacyEnabled: MutableStateFlow<Boolean> = MutableStateFlow(initValue)
    override val screenLockInSeconds: MutableStateFlow<ScreenLockSetting> =
        MutableStateFlow(ScreenLockSetting.LOCK_IMMEDIATELY)
    override val torEnabled: MutableStateFlow<Boolean> = MutableStateFlow(initValue)
    override val proxyEnabled: MutableStateFlow<Boolean> = MutableStateFlow(initValue)
    override val proxyUrl: MutableStateFlow<String> = MutableStateFlow("")
    override val rememberHardwareDevices: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val testnetEnabled: MutableStateFlow<Boolean> = MutableStateFlow(initValue)
    override val experimentalFeaturesEnabled: MutableStateFlow<Boolean> = MutableStateFlow(initValue)
    override val analyticsEnabled: MutableStateFlow<Boolean> = MutableStateFlow(initValue)
    override val electrumNodeEnabled: MutableStateFlow<Boolean> = MutableStateFlow(initValue)
    override val personalElectrumServerTlsEnabled: MutableStateFlow<Boolean> = MutableStateFlow(initValue)
    override val personalBitcoinElectrumServer: MutableStateFlow<String> = MutableStateFlow("")
    override val personalLiquidElectrumServer: MutableStateFlow<String> = MutableStateFlow("")
    override val personalTestnetElectrumServer: MutableStateFlow<String> = MutableStateFlow("")
    override val personalTestnetLiquidElectrumServer: MutableStateFlow<String> = MutableStateFlow("")
    override val electrumServerGapLimit: MutableStateFlow<String> = MutableStateFlow("")
    override val locales: MutableStateFlow<Map<String?, String?>> = MutableStateFlow(mapOf("en" to "English"))
    override val locale: MutableStateFlow<String?> = MutableStateFlow("en")
}