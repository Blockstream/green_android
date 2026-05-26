package com.blockstream.compose.models.settings

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_app_settings
import blockstream_green.common.generated.resources.id_applies_to_every_wallet
import blockstream_green.common.generated.resources.id_enter_a_valid_proxy_address
import blockstream_green.common.generated.resources.id_invalid_gap_limit
import blockstream_green.common.generated.resources.id_invalid_server_address_format
import com.blockstream.compose.events.Event
import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffect
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.data.ApplicationSettings
import com.blockstream.data.data.ScreenLockSetting
import com.blockstream.data.extensions.isHostPortFormatValid
import com.blockstream.data.extensions.isNotBlank
import com.blockstream.data.gdk.events.GenericEvent
import com.blockstream.data.managers.LocaleManager
import com.blockstream.data.managers.Locales
import com.blockstream.utils.Loggable
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
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
    abstract val proxyUrlError: MutableStateFlow<String?>
    abstract val rememberHardwareDevices: MutableStateFlow<Boolean>
    abstract val testnetEnabled: MutableStateFlow<Boolean>
    abstract val experimentalFeaturesEnabled: MutableStateFlow<Boolean>
    abstract val analyticsEnabled: MutableStateFlow<Boolean>
    abstract val electrumNodeEnabled: MutableStateFlow<Boolean>
    abstract val personalElectrumServerTlsEnabled: MutableStateFlow<Boolean>
    abstract val personalBitcoinElectrumServer: MutableStateFlow<String>
    abstract val personalBitcoinElectrumServerError: MutableStateFlow<String?>
    abstract val personalLiquidElectrumServer: MutableStateFlow<String>
    abstract val personalLiquidElectrumServerError: MutableStateFlow<String?>
    abstract val personalTestnetElectrumServer: MutableStateFlow<String>
    abstract val personalTestnetElectrumServerError: MutableStateFlow<String?>
    abstract val personalTestnetLiquidElectrumServer: MutableStateFlow<String>
    abstract val personalTestnetLiquidElectrumServerError: MutableStateFlow<String?>
    abstract val electrumServerGapLimit: MutableStateFlow<String>
    abstract val electrumServerGapLimitError: MutableStateFlow<String?>
    abstract val customGapLimitEnabled: MutableStateFlow<Boolean>
    abstract val locales: MutableStateFlow<Map<String?, String?>>
    abstract val locale: MutableStateFlow<String?>

    abstract fun onResetProxySettings()
    abstract fun onResetElectrumServerSettings()
    abstract fun onResetGapLimit()

    companion object {
        const val SCAN_GAP_LIMIT_MIN = 1
        const val SCAN_GAP_LIMIT_MAX = 1000
        const val DEFAULT_SCAN_GAP_LIMIT = "20"
        const val DEFAULT_BITCOIN_ELECTRUM_URL = "blockstream.info:700"
        const val DEFAULT_LIQUID_ELECTRUM_URL = "blockstream.info:995"
        const val DEFAULT_TESTNET_ELECTRUM_URL = "blockstream.info:993"
        const val DEFAULT_TESTNET_LIQUID_ELECTRUM_URL = "blockstream.info:465"

        const val DEFAULT_IP_AND_PORT = "192.168.1.10:9050"

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
    override val proxyEnabled = MutableStateFlow(appSettings.proxyEnabled)
    override val proxyUrl = MutableStateFlow(appSettings.proxyUrl ?: "")
    override val proxyUrlError = MutableStateFlow<String?>(null)
    override val rememberHardwareDevices = MutableStateFlow(appSettings.rememberHardwareDevices)
    override val testnetEnabled = MutableStateFlow(appSettings.testnet)
    override val experimentalFeaturesEnabled = MutableStateFlow(appSettings.experimentalFeatures)
    override val analyticsEnabled = MutableStateFlow(appSettings.analytics)
    override val electrumNodeEnabled = MutableStateFlow(appSettings.electrumNode)

    override val personalElectrumServerTlsEnabled: MutableStateFlow<Boolean> =
        MutableStateFlow(appSettings.personalElectrumServerTls)
    override val personalBitcoinElectrumServer: MutableStateFlow<String> =
        MutableStateFlow(appSettings.personalBitcoinElectrumServer ?: "")
    override val personalBitcoinElectrumServerError = MutableStateFlow<String?>(null)

    override val personalLiquidElectrumServer: MutableStateFlow<String> =
        MutableStateFlow(appSettings.personalLiquidElectrumServer ?: "")
    override val personalLiquidElectrumServerError = MutableStateFlow<String?>(null)
    override val personalTestnetElectrumServerError = MutableStateFlow<String?>(null)
    override val personalTestnetElectrumServer: MutableStateFlow<String> =
        MutableStateFlow(appSettings.personalTestnetElectrumServer ?: "")
    override val personalTestnetLiquidElectrumServer: MutableStateFlow<String> =
        MutableStateFlow(appSettings.personalTestnetLiquidElectrumServer ?: "")
    override val personalTestnetLiquidElectrumServerError = MutableStateFlow<String?>(null)

    override val electrumServerGapLimit: MutableStateFlow<String> =
        MutableStateFlow(appSettings.electrumServerGapLimit?.toString() ?: "")
    override val electrumServerGapLimitError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val customGapLimitEnabled: MutableStateFlow<Boolean> =
        MutableStateFlow(appSettings.customGapLimitEnabled)
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

    private data class HostPortUrlField(
        val url: MutableStateFlow<String>,
        val errorState: MutableStateFlow<String?>,
        val errorRes: StringResource
    )

    init {

        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_app_settings),
                subtitle = getString(Res.string.id_applies_to_every_wallet),
                isCentered = true,
            )
            database.insertEvent(GenericEvent(deviceId = settingsManager.getCountlyDeviceId()).sha256(), randomInsert = true)
        }

        combine<Any?, Any?>(
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

        setupHostPortUrlValidation(
            enabled = proxyEnabled,
            fields = listOf(
                HostPortUrlField(
                    proxyUrl,
                    proxyUrlError,
                    Res.string.id_enter_a_valid_proxy_address
                )
            )
        )

        setupHostPortUrlValidation(
            enabled = electrumNodeEnabled,
            fields = listOf(
                HostPortUrlField(
                    personalBitcoinElectrumServer,
                    personalBitcoinElectrumServerError,
                    Res.string.id_invalid_server_address_format
                ),
                HostPortUrlField(
                    personalLiquidElectrumServer,
                    personalLiquidElectrumServerError,
                    Res.string.id_invalid_server_address_format
                )
            )
        )

        val testnetServersEnabled = combine(
            electrumNodeEnabled,
            testnetEnabled
        ) { node, testnet -> node && testnet }

        setupHostPortUrlValidation(
            enabled = testnetServersEnabled,
            fields = listOf(
                HostPortUrlField(
                    personalTestnetElectrumServer,
                    personalTestnetElectrumServerError,
                    Res.string.id_invalid_server_address_format
                ),
                HostPortUrlField(
                    personalTestnetLiquidElectrumServer,
                    personalTestnetLiquidElectrumServerError,
                    Res.string.id_invalid_server_address_format
                )
            )
        )

        setupGapLimitValidation()

        bootstrap()
    }

    private fun String.isGapLimitValid(): Boolean {
        if (this.isBlank()) return true
        val value = this.toIntOrNull()
        return value != null && value in SCAN_GAP_LIMIT_MIN..SCAN_GAP_LIMIT_MAX
    }

    @OptIn(FlowPreview::class)
    private fun setupHostPortUrlValidation(
        enabled: Flow<Boolean>,
        fields: List<HostPortUrlField>
    ) {
        fields.forEach { field ->
            combine(enabled, field.url) { isEnabled, text -> isEnabled to text }
                .debounce(500)
                .onEach { (isEnabled, text) ->
                    if (!isEnabled || text.isBlank()) {
                        field.errorState.value = null
                        return@onEach
                    }
                    val isInvalid = isEnabled && text.isHostPortFormatValid().not()
                    field.errorState.value = if (isInvalid) getString(field.errorRes) else null
                }.launchIn(viewModelScope)
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupGapLimitValidation() {
        electrumServerGapLimit.debounce(500).onEach { limit ->
            if (limit.isGapLimitValid()) {
                electrumServerGapLimitError.value = null
            } else {
                electrumServerGapLimitError.value = getString(
                    Res.string.id_invalid_gap_limit,
                    SCAN_GAP_LIMIT_MIN,
                    SCAN_GAP_LIMIT_MAX
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun isConfigValid(): Boolean {
        val proxyValid = !proxyEnabled.value || (proxyUrl.value.isNotBlank() && proxyUrl.value.isHostPortFormatValid())
        val gapLimitValid = !customGapLimitEnabled.value || electrumServerGapLimit.value.isGapLimitValid()

        val btcValid = !electrumNodeEnabled.value || personalBitcoinElectrumServer.value.isHostPortFormatValid()
        val liqValid = !electrumNodeEnabled.value || personalLiquidElectrumServer.value.isHostPortFormatValid()

        val testnetServersEnabled = electrumNodeEnabled.value && testnetEnabled.value
        val tBtcValid = !testnetServersEnabled || personalTestnetElectrumServer.value.isHostPortFormatValid()
        val tLiqValid = !testnetServersEnabled || personalTestnetLiquidElectrumServer.value.isHostPortFormatValid()

        return proxyValid && gapLimitValid && btcValid && liqValid && tBtcValid && tLiqValid
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.Save -> {
                if (isConfigValid()) {
                    localeManager.setLocale(locale.value)
                    settingsManager.saveApplicationSettings(getSettings())
                    postSideEffect(SideEffects.NavigateBack())
                }
            }

            is LocalEvents.AutoSave -> {
                if (isConfigValid()) {
                    localeManager.setLocale(locale.value)
                    val currentSettings = getSettings()
                    settingsManager.saveApplicationSettings(currentSettings)
                    appSettings = currentSettings
                }
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
        rememberHardwareDevices = rememberHardwareDevices.value,
        electrumNode = electrumNodeEnabled.value,
        tor = torEnabled.value,
        personalElectrumServerTls = personalElectrumServerTlsEnabled.value,

        proxyEnabled = proxyEnabled.value,
        proxyUrl = proxyUrl.value.ifBlank { null },

        customGapLimitEnabled = customGapLimitEnabled.value,
        electrumServerGapLimit = electrumServerGapLimit.value.toIntOrNull(),

        personalBitcoinElectrumServer = personalBitcoinElectrumServer.value.ifBlank { null },
        personalLiquidElectrumServer = personalLiquidElectrumServer.value.ifBlank { null },
        personalTestnetElectrumServer = personalTestnetElectrumServer.value.ifBlank { null },
        personalTestnetLiquidElectrumServer = personalTestnetLiquidElectrumServer.value.ifBlank { null }
    )

    private fun areSettingsDirty(): Boolean {
        return getSettings() != appSettings
    }

    override fun onResetProxySettings() {
        proxyUrl.value = ""
        proxyUrlError.value = null
    }

    override fun onResetElectrumServerSettings() {
        personalBitcoinElectrumServer.value = ""
        personalBitcoinElectrumServerError.value = null
        personalLiquidElectrumServer.value = ""
        personalLiquidElectrumServerError.value = null
        personalTestnetElectrumServer.value = ""
        personalTestnetElectrumServerError.value = null
        personalTestnetLiquidElectrumServer.value = ""
        personalTestnetLiquidElectrumServerError.value = null
        personalElectrumServerTlsEnabled.value = true
    }

    override fun onResetGapLimit() {
        electrumServerGapLimit.value = DEFAULT_SCAN_GAP_LIMIT
        electrumServerGapLimitError.value = null
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
    override val proxyUrlError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val rememberHardwareDevices: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val testnetEnabled: MutableStateFlow<Boolean> = MutableStateFlow(initValue)
    override val experimentalFeaturesEnabled: MutableStateFlow<Boolean> = MutableStateFlow(initValue)
    override val analyticsEnabled: MutableStateFlow<Boolean> = MutableStateFlow(initValue)
    override val electrumNodeEnabled: MutableStateFlow<Boolean> = MutableStateFlow(initValue)
    override val personalElectrumServerTlsEnabled: MutableStateFlow<Boolean> = MutableStateFlow(initValue)
    override val personalBitcoinElectrumServer: MutableStateFlow<String> = MutableStateFlow("")
    override val personalBitcoinElectrumServerError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val personalLiquidElectrumServer: MutableStateFlow<String> = MutableStateFlow("")
    override val personalLiquidElectrumServerError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val personalTestnetElectrumServer: MutableStateFlow<String> = MutableStateFlow("")
    override val personalTestnetElectrumServerError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val personalTestnetLiquidElectrumServer: MutableStateFlow<String> = MutableStateFlow("")
    override val personalTestnetLiquidElectrumServerError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val electrumServerGapLimit: MutableStateFlow<String> = MutableStateFlow("")
    override val electrumServerGapLimitError: MutableStateFlow<String?> = MutableStateFlow(null)
    override val customGapLimitEnabled: MutableStateFlow<Boolean> = MutableStateFlow(initValue)
    override val locales: MutableStateFlow<Map<String?, String?>> = MutableStateFlow(mapOf("en" to "English"))
    override val locale: MutableStateFlow<String?> = MutableStateFlow("en")
    override fun onResetProxySettings() {}
    override fun onResetElectrumServerSettings() {}
    override fun onResetGapLimit() {}
}