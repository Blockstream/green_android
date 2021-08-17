package com.blockstream.green.ui.settings

import androidx.lifecycle.MutableLiveData
import com.blockstream.green.lifecycle.ListenableLiveData
import com.blockstream.green.settings.ApplicationSettings
import com.blockstream.green.settings.SettingsManager
import com.blockstream.green.ui.AppViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : AppViewModel() {
    private var appSettings: ApplicationSettings = settingsManager.getApplicationSettings()

    val proxyURLInvalid = MutableLiveData(true)

    val proxyURL = ListenableLiveData(appSettings.proxyUrl ?: "") {
        proxyURLInvalid.postValue(it.isBlank())
    }

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

    val personalBitcoinElectrumServer = MutableLiveData(appSettings.personalBitcoinElectrumServer ?: DEFAULT_BITCOIN_ELECTRUM_URL)
    val personalLiquidElectrumServer = MutableLiveData(appSettings.personalLiquidElectrumServer ?: DEFAULT_LIQUID_ELECTRUM_URL)
    val personalTestnetElectrumServer = MutableLiveData(appSettings.personalTestnetElectrumServer ?: DEFAULT_TESTNET_ELECTRUM_URL)

    val spvBitcoinElectrumServer = MutableLiveData(appSettings.spvBitcoinElectrumServer ?: DEFAULT_MULTI_SPV_URL )
    val spvLiquidElectrumServer = MutableLiveData(appSettings.spvLiquidElectrumServer ?: DEFAULT_MULTI_SPV_URL)
    val spvTestnetElectrumServer = MutableLiveData(appSettings.spvTestnetElectrumServer ?: DEFAULT_MULTI_SPV_URL)

    fun getSettings() = ApplicationSettings(
        proxyUrl = if (enableProxy.value == true && !proxyURL.value.isNullOrBlank()) proxyURL.value else null,
        electrumNode = enableElectrumNode?.value ?: false,
        tor = enableTorRouting.value ?: false,
        spv = enableSPV?.value ?: false,
        multiServerValidation = enableMultiServerValidation?.value ?: false,

        // use null value as a reset to re-set the default urls and blank as a way to disabled it for a specific network
        personalBitcoinElectrumServer = if(enableElectrumNode?.value == true) personalBitcoinElectrumServer.value else null,
        personalLiquidElectrumServer = if(enableElectrumNode?.value == true) personalLiquidElectrumServer.value else null,
        personalTestnetElectrumServer = if(enableElectrumNode?.value == true) personalTestnetElectrumServer.value else null,

        spvBitcoinElectrumServer = if(enableSPV?.value == true) spvBitcoinElectrumServer.value else null,
        spvLiquidElectrumServer = if(enableSPV?.value == true) spvLiquidElectrumServer.value else null,
        spvTestnetElectrumServer = if(enableSPV?.value == true) spvTestnetElectrumServer.value else null
    )

    fun saveSettings(){
        settingsManager.saveApplicationSettings(getSettings())
    }

    companion object {
        const val DEFAULT_BITCOIN_ELECTRUM_URL = "blockstream.info:700"
        const val DEFAULT_LIQUID_ELECTRUM_URL = "blockstream.info:995"
        const val DEFAULT_TESTNET_ELECTRUM_URL = "blockstream.info:993"
        const val DEFAULT_TESTNET_LIQUID_ELECTRUM_URL = "blockstream.info:465"

        const val DEFAULT_MULTI_SPV_URL = "blockstream.info:993, electrum.blockstream.info:60002"
    }
}