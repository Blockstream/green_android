package com.blockstream.compose.models.lightning

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_get_more_out_of_jade
import blockstream_green.common.generated.resources.id_lightning_network
import blockstream_green.common.generated.resources.id_scaling_solution_for_faster
import blockstream_green.common.generated.resources.id_unlock_lightning
import com.blockstream.compose.events.Event
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.models.jade.JadeQrOperation
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.compose.utils.StringHolder
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.devices.DeviceModel

enum class OnboardingMode { HOT_WALLET, JADE_WALLET }

abstract class LightningOnboardingViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    abstract val title: StringHolder
    abstract val subtitle: StringHolder
}

class LightningOnboardingViewModel(greenWallet: GreenWallet) :
    LightningOnboardingViewModelAbstract(greenWallet = greenWallet) {
    override fun screenName(): String = "LightningOnboarding"

    val mode: OnboardingMode = if (greenWallet.isHardware) OnboardingMode.JADE_WALLET else OnboardingMode.HOT_WALLET

    override val title = if (greenWallet.isHardware) {
        StringHolder.create(Res.string.id_get_more_out_of_jade)
    } else {
        StringHolder.create(Res.string.id_lightning_network)
    }

    override val subtitle = if (greenWallet.isHardware) {
        StringHolder.create(Res.string.id_unlock_lightning)
    } else {
        StringHolder.create(Res.string.id_scaling_solution_for_faster)
    }

    class LocalEvents {
        object EnableLightning : Event
    }

    init {
        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            LocalEvents.EnableLightning -> {
                if (mode == OnboardingMode.JADE_WALLET) {
                    if (session.isHwWatchOnly && !greenWallet.isWatchOnlyQr) {
                        postSideEffect(
                            SideEffects.NavigateTo(
                                NavigateDestinations.DeviceScan(
                                    greenWallet = greenWallet,
                                    isWatchOnlyUpgrade = true
                                )
                            )
                        )
                    } else {
                        postSideEffect(
                            SideEffects.NavigateTo(
                                NavigateDestinations.JadeQR(
                                    greenWalletOrNull = greenWallet,
                                    operation = JadeQrOperation.LightningMnemonicExport,
                                    deviceModel = DeviceModel.BlockstreamGeneric
                                )
                            )
                        )
                    }
                } else {
                    postSideEffect(SideEffects.Success(true))
                }
            }
        }
    }
}