package com.blockstream.common.models.settings

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_app_settings
import com.blockstream.common.data.ApplicationSettings
import com.blockstream.common.data.ScreenLockSetting
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.gdk.events.GenericEvent
import com.blockstream.common.managers.LocaleManager
import com.blockstream.common.managers.Locales
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.utils.Loggable
import com.blockstream.ui.events.Event
import com.blockstream.ui.navigation.NavData
import com.blockstream.ui.sideeffects.SideEffect
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

abstract class AppSettingsViewModelAbstract() :
    GreenViewModel() {
    override fun screenName(): String = "AppSettings"

    abstract val multiServerValidationFeatureEnabled: Boolean

    abstract val analyticsFeatureEnabled: Boolean

    abstract val experimentalFeatureEnabled: Boolean

    @NativeCoroutinesState
    abstract val enhancedPrivacyEnabled: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val screenLockInSeconds: MutableStateFlow<ScreenLockSetting>

    @NativeCoroutinesState
    abstract val torEnabled: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val proxyEnabled: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val proxyUrl: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val rememberHardwareDevices: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val testnetEnabled: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val experimentalFeaturesEnabled: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val analyticsEnabled: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val electrumNodeEnabled: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val personalElectrumServerTlsEnabled: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val personalBitcoinElectrumServer: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val personalLiquidElectrumServer: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val personalTestnetElectrumServer: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val personalTestnetLiquidElectrumServer: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val electrumServerGapLimit: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val locales: MutableStateFlow<Map<String?, String?>>

    @NativeCoroutinesState
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

    @NativeCoroutinesState
    override val enhancedPrivacyEnabled = MutableStateFlow(viewModelScope, appSettings.enhancedPrivacy)

    @NativeCoroutinesState
    override val screenLockInSeconds = MutableStateFlow(viewModelScope, ScreenLockSetting.bySeconds(appSettings.screenLockInSeconds))

    @NativeCoroutinesState
    override val torEnabled = MutableStateFlow(viewModelScope, appSettings.tor)

    @NativeCoroutinesState
    override val proxyEnabled = MutableStateFlow(viewModelScope, appSettings.proxyUrl.isNotBlank())

    @NativeCoroutinesState
    override val proxyUrl = MutableStateFlow(viewModelScope, appSettings.proxyUrl ?: "")

    @NativeCoroutinesState
    override val rememberHardwareDevices = MutableStateFlow(viewModelScope, appSettings.rememberHardwareDevices)

    @NativeCoroutinesState
    override val testnetEnabled = MutableStateFlow(viewModelScope, appSettings.testnet)

    @NativeCoroutinesState
    override val experimentalFeaturesEnabled = MutableStateFlow(viewModelScope, appSettings.experimentalFeatures)

    @NativeCoroutinesState
    override val analyticsEnabled = MutableStateFlow(viewModelScope, appSettings.analytics)

    @NativeCoroutinesState
    override val electrumNodeEnabled = MutableStateFlow(viewModelScope, appSettings.electrumNode)


    @NativeCoroutinesState
    override val personalElectrumServerTlsEnabled: MutableStateFlow<Boolean> =
        MutableStateFlow(viewModelScope, appSettings.personalElectrumServerTls)

    @NativeCoroutinesState
    override val personalBitcoinElectrumServer: MutableStateFlow<String> =
        MutableStateFlow(viewModelScope, appSettings.personalBitcoinElectrumServer ?: "")

    @NativeCoroutinesState
    override val personalLiquidElectrumServer: MutableStateFlow<String> =
        MutableStateFlow(viewModelScope, appSettings.personalLiquidElectrumServer ?: "")

    @NativeCoroutinesState
    override val personalTestnetElectrumServer: MutableStateFlow<String> =
        MutableStateFlow(viewModelScope, appSettings.personalTestnetElectrumServer ?: "")

    @NativeCoroutinesState
    override val personalTestnetLiquidElectrumServer: MutableStateFlow<String> =
        MutableStateFlow(viewModelScope, appSettings.personalTestnetLiquidElectrumServer ?: "")

    override val electrumServerGapLimit: MutableStateFlow<String> =
        MutableStateFlow(viewModelScope, "${appSettings.electrumServerGapLimit ?: ""}")

    @NativeCoroutinesState
    override val locales = MutableStateFlow(viewModelScope, Locales)

    @NativeCoroutinesState
    override val locale: MutableStateFlow<String?> = MutableStateFlow(viewModelScope, localeManager.getLocale())

    class LocalEvents {
        object AnalyticsMoreInfo : Events.EventSideEffect(SideEffects.NavigateTo(NavigateDestinations.Analytics))
        object Save : Event
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
        }.launchIn(viewModelScope.coroutineScope)

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
        electrumServerGapLimit = electrumServerGapLimit.value.takeIf { it.isNotBlank() }?.toIntOrNull(),

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
    override val enhancedPrivacyEnabled: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, initValue)
    override val screenLockInSeconds: MutableStateFlow<ScreenLockSetting> =
        MutableStateFlow(viewModelScope, ScreenLockSetting.LOCK_IMMEDIATELY)
    override val torEnabled: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, initValue)
    override val proxyEnabled: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, initValue)
    override val proxyUrl: MutableStateFlow<String> = MutableStateFlow(viewModelScope, "")
    override val rememberHardwareDevices: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, true)
    override val testnetEnabled: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, initValue)
    override val experimentalFeaturesEnabled: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, initValue)
    override val analyticsEnabled: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, initValue)
    override val electrumNodeEnabled: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, initValue)
    override val personalElectrumServerTlsEnabled: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, initValue)
    override val personalBitcoinElectrumServer: MutableStateFlow<String> = MutableStateFlow(viewModelScope, "")
    override val personalLiquidElectrumServer: MutableStateFlow<String> = MutableStateFlow(viewModelScope, "")
    override val personalTestnetElectrumServer: MutableStateFlow<String> = MutableStateFlow(viewModelScope, "")
    override val personalTestnetLiquidElectrumServer: MutableStateFlow<String> = MutableStateFlow(viewModelScope, "")
    override val electrumServerGapLimit: MutableStateFlow<String> = MutableStateFlow(viewModelScope, "")
    override val locales: MutableStateFlow<Map<String?, String?>> = MutableStateFlow(viewModelScope, mapOf("en" to "English"))
    override val locale: MutableStateFlow<String?> = MutableStateFlow(viewModelScope, "en")
}