package com.blockstream.common.models.devices

import com.blockstream.common.Urls
import com.blockstream.common.data.WatchOnlyCredentials
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.events.Event
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.jade.JadeQrOperation
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class ImportPubKeyViewModelAbstract(val deviceBrand: DeviceBrand) : GreenViewModel() {
    override fun screenName(): String = "ImportPubKey"

    @NativeCoroutinesState
    abstract val canUseBiometrics: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val withBiometrics: MutableStateFlow<Boolean>
}

class ImportPubKeyViewModel constructor(deviceBrand: DeviceBrand) :
    ImportPubKeyViewModelAbstract(deviceBrand = deviceBrand) {

    override val canUseBiometrics: MutableStateFlow<Boolean>
    override val withBiometrics: MutableStateFlow<Boolean>

    private val isTestnetEnabled
        get() = settingsManager.getApplicationSettings().testnet

    private var isTestnet = false

    class LocalEvents {
        object ScanXpub : Event
        object LearnMore : Event
        data class ImportPubKey(val pubKey: String) : Event
        data class SelectEnviroment(val isTestnet: Boolean, val customNetwork: Network?): Event
    }

    init {
        greenKeystore.canUseBiometrics().also {
            canUseBiometrics = MutableStateFlow(it)
            withBiometrics = MutableStateFlow(it)
        }
        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ScanXpub -> {

                if (isTestnetEnabled) {
                    postSideEffect(SideEffects.SelectEnvironment)
                } else {
                    postSideEffect(
                        SideEffects.NavigateTo(
                            NavigateDestinations.JadeQR(
                                operation = JadeQrOperation.ExportXpub,
                                deviceBrand = deviceBrand
                            )
                        )
                    )
                }
            }

            is LocalEvents.SelectEnviroment -> {

                isTestnet = event.isTestnet

                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.JadeQR(
                            operation = JadeQrOperation.ExportXpub,
                            deviceBrand = deviceBrand
                        )
                    )
                )

            }

            is LocalEvents.LearnMore -> {
                postSideEffect(
                    SideEffects.OpenBrowser(
                        if (deviceBrand.isJade) Urls.HELP_JADE_EXPORT_XPUB else Urls.HELP_HW_EXPORT_XPUB
                    )
                )
            }

            is LocalEvents.ImportPubKey -> {
                createNewWatchOnlyWallet(
                    network = session.networks.bitcoinElectrum(isTestnet),
                    persistLoginCredentials = true,
                    watchOnlyCredentials = WatchOnlyCredentials(coreDescriptors = listOf(event.pubKey)),
                    withBiometrics = withBiometrics.value,
                    deviceBrand = deviceBrand
                )
            }
        }
    }


    companion object : Loggable()
}

class ImportPubKeyViewModelPreview :
    ImportPubKeyViewModelAbstract(deviceBrand = DeviceBrand.Blockstream) {

    override val canUseBiometrics: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val withBiometrics: MutableStateFlow<Boolean> = MutableStateFlow(true)

    companion object {
        fun preview() =
            DeviceInfoViewModelPreview()
    }

}