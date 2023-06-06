package com.blockstream.green.ui.settings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blockstream.common.data.ApplicationSettings
import com.blockstream.common.data.ScreenLockSetting
import com.blockstream.common.managers.SettingsManager
import com.blockstream.green.data.Countly
import com.blockstream.green.lifecycle.ListenableLiveData
import com.blockstream.green.ui.AppViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    countly: Countly
) : AppViewModel(countly) {
    private var appSettings: ApplicationSettings = settingsManager.getApplicationSettings()

    val analyticsFeatureEnabled = settingsManager.analyticsFeatureEnabled
    val lightningEnabled
        get() = settingsManager.isLightningEnabled(countly)

    val proxyURLInvalid = MutableLiveData(true)

    val proxyURL = ListenableLiveData(appSettings.proxyUrl ?: "") {
        proxyURLInvalid.postValue(it.isBlank())
    }

    val enableEnhancedPrivacy = MutableLiveData(appSettings.enhancedPrivacy)
    // screenLockSetting must be optional as is accessed by enableEnhancedPrivacy before being initialized
    val screenLockSetting : MutableLiveData<Int> = MutableLiveData(appSettings.screenLockInSeconds)
    val enableTestnet = MutableLiveData(appSettings.testnet)
    val enableExperimentalFeatures = MutableLiveData(appSettings.experimentalFeatures)
    val enableAnalytics = MutableLiveData(appSettings.analytics)
    val enableTorRouting = MutableLiveData(appSettings.tor)
    val enableProxy = MutableLiveData(appSettings.proxyUrl != null)

    val enableElectrumNode: MutableLiveData<Boolean> = MutableLiveData(appSettings.electrumNode)

    // enableSPV, enableMultiServerValidation must be optional as both are initialized at the same time
    val enableSPV: MutableLiveData<Boolean>? = ListenableLiveData(appSettings.spv){
        if(it == false && enableMultiServerValidation?.value == true){
            enableMultiServerValidation.postValue(false)
        }
    }

    val enableMultiServerValidation : MutableLiveData<Boolean>? = ListenableLiveData(appSettings.multiServerValidation) {
        if(it && enableSPV?.value == false){
            enableSPV.postValue(true)
        }
    }

    val personalBitcoinElectrumServer = MutableLiveData(appSettings.personalBitcoinElectrumServer ?: "")
    val personalLiquidElectrumServer = MutableLiveData(appSettings.personalLiquidElectrumServer ?: "")
    val personalTestnetElectrumServer = MutableLiveData(appSettings.personalTestnetElectrumServer ?: "")
    val personalTestnetLiquidElectrumServer = MutableLiveData(appSettings.personalTestnetLiquidElectrumServer ?: "")

    val spvBitcoinElectrumServer = MutableLiveData(appSettings.spvBitcoinElectrumServer ?: "" )
    val spvLiquidElectrumServer = MutableLiveData(appSettings.spvLiquidElectrumServer ?: "")
    val spvTestnetElectrumServer = MutableLiveData(appSettings.spvTestnetElectrumServer ?: "")
    val spvTestnetLiquidElectrumServer = MutableLiveData(appSettings.spvTestnetLiquidElectrumServer ?: "")

    init {
        settingsManager.appSettingsStateFlow.onEach {
            appSettings = it

            // Only analytics is changed from an outside scope
            enableAnalytics.postValue(appSettings.analytics)
        }.launchIn(viewModelScope)
    }

    fun getSettings() = ApplicationSettings(
        enhancedPrivacy = enableEnhancedPrivacy.value ?: false,
        screenLockInSeconds = screenLockSetting.value ?: ScreenLockSetting.LOCK_IMMEDIATELY.seconds,
        testnet = enableTestnet.value ?: false,
        analytics = enableAnalytics.value ?: false,
        experimentalFeatures = enableExperimentalFeatures.value ?: false,
        proxyUrl = if (enableProxy.value == true && !proxyURL.value.isNullOrBlank()) proxyURL.value else null,
        electrumNode = enableElectrumNode.value ?: false,
        tor = enableTorRouting.value ?: false,
        spv = enableSPV?.value ?: false,
        multiServerValidation = enableMultiServerValidation?.value ?: false,

        // use null value as a reset to re-set the default urls and blank as a way to disabled it for a specific network
        personalBitcoinElectrumServer = if(enableElectrumNode.value == true) personalBitcoinElectrumServer.value else null,
        personalLiquidElectrumServer = if(enableElectrumNode.value == true) personalLiquidElectrumServer.value else null,
        personalTestnetElectrumServer = if(enableElectrumNode.value == true) personalTestnetElectrumServer.value else null,
        personalTestnetLiquidElectrumServer = if(enableElectrumNode.value == true) personalTestnetLiquidElectrumServer.value else null,

        spvBitcoinElectrumServer = if(enableSPV?.value == true) spvBitcoinElectrumServer.value else null,
        spvLiquidElectrumServer = if(enableSPV?.value == true) spvLiquidElectrumServer.value else null,
        spvTestnetElectrumServer = if(enableSPV?.value == true) spvTestnetElectrumServer.value else null,
        spvTestnetLiquidElectrumServer = if(enableSPV?.value == true) spvTestnetLiquidElectrumServer.value else null,
    )

    fun saveSettings(){
        settingsManager.saveApplicationSettings(getSettings())
    }

    fun areSettingsDirty(): Boolean {
        return getSettings() != appSettings
    }

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