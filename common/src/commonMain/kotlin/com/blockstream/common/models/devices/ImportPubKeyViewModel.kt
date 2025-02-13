package com.blockstream.common.models.devices

import com.blockstream.common.Urls
import com.blockstream.common.data.WatchOnlyCredentials
import com.blockstream.common.devices.DeviceModel
import com.blockstream.ui.events.Event
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.jade.JadeQrOperation
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.green.utils.Loggable
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class ImportPubKeyViewModelAbstract(val deviceModel: DeviceModel) : GreenViewModel() {
    override fun screenName(): String = "ImportPubKey"
}

class ImportPubKeyViewModel constructor(deviceModel: DeviceModel) :
    ImportPubKeyViewModelAbstract(deviceModel = deviceModel) {

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
        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ScanXpub -> {

                if (isTestnetEnabled) {
                    postSideEffect(SideEffects.NavigateTo(NavigateDestinations.Environment))
                } else {
                    postSideEffect(
                        SideEffects.NavigateTo(
                            NavigateDestinations.JadeQR(
                                greenWalletOrNull = greenWalletOrNull,
                                operation = JadeQrOperation.ExportXpub,
                                deviceModel = deviceModel
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
                            greenWalletOrNull = greenWalletOrNull,
                            operation = JadeQrOperation.ExportXpub,
                            deviceModel = deviceModel
                        )
                    )
                )

            }

            is LocalEvents.LearnMore -> {
                postSideEffect(
                    SideEffects.OpenBrowser(
                        if (deviceModel.isJade) Urls.HELP_JADE_EXPORT_XPUB else Urls.HELP_HW_EXPORT_XPUB
                    )
                )
            }

            is LocalEvents.ImportPubKey -> {
                createNewWatchOnlyWallet(
                    network = session.networks.bitcoinElectrum(isTestnet),
                    persistLoginCredentials = true,
                    watchOnlyCredentials = WatchOnlyCredentials(coreDescriptors = listOf(event.pubKey)),
                    deviceModel = deviceModel
                )
            }
        }
    }


    companion object : Loggable()
}

class ImportPubKeyViewModelPreview :
    ImportPubKeyViewModelAbstract(deviceModel = DeviceModel.BlockstreamGeneric) {

    companion object {
        fun preview() =
            DeviceInfoViewModelPreview()
    }

}