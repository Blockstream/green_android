package com.blockstream.compose.utils

import com.blockstream.compose.events.Events
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.models.jade.JadeQrOperation
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.data.devices.DeviceModel

object SwapUtils {
    fun navigateToDeviceScanOrJadeQr(viewModel: GreenViewModel) {
        viewModel.postEvent(Events.SwapSetup)

        if (viewModel.session.isHwWatchOnly && !viewModel.greenWallet.isWatchOnlyQr) {
            viewModel.postEvent(
                NavigateDestinations.DeviceScan(
                    greenWallet = viewModel.greenWallet, isWatchOnlyUpgrade = true
                )
            )
        } else {
            viewModel.postEvent(
                Events.NavigateTo(
                    NavigateDestinations.JadeQR(
                        greenWalletOrNull = viewModel.greenWallet,
                        operation = JadeQrOperation.BoltzMnemonicExport,
                        deviceModel = viewModel.session.deviceModel ?: DeviceModel.BlockstreamGeneric
                    )
                )
            )
        }
    }
}