package com.blockstream.compose.models.devices

import com.blockstream.data.Urls
import com.blockstream.data.data.WatchOnlyCredentials
import com.blockstream.data.devices.DeviceModel
import com.blockstream.data.gdk.data.Network
import com.blockstream.compose.events.Event
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.models.jade.JadeQrOperation
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.utils.Loggable

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
        data class SelectEnviroment(val isTestnet: Boolean, val customNetwork: Network?) : Event
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