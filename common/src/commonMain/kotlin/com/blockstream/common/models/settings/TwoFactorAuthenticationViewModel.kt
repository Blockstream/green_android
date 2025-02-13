package com.blockstream.common.models.settings

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_twofactor_authentication
import com.blockstream.common.data.GreenWallet
import com.blockstream.ui.navigation.NavData
import com.blockstream.common.extensions.previewNetwork
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.models.GreenViewModel
import com.rickclephas.kmp.observableviewmodel.launch
import org.jetbrains.compose.resources.getString

abstract class TwoFactorAuthenticationViewModelAbstract(
    greenWallet: GreenWallet,

    ) : GreenViewModel(greenWalletOrNull = greenWallet) {
    abstract val networks: List<Network>
}

class TwoFactorAuthenticationViewModel(
    greenWallet: GreenWallet,
) : TwoFactorAuthenticationViewModelAbstract(
    greenWallet = greenWallet
) {
    override fun screenName(): String = "WalletSettings2FA"

    override val networks =
        listOfNotNull(sessionOrNull?.activeBitcoinMultisig, sessionOrNull?.activeLiquidMultisig)

    init {
        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_twofactor_authentication),
                subtitle = greenWallet.name
            )
        }

        bootstrap()
    }
}

class TwoFactorAuthenticationViewModelPreview(
    greenWallet: GreenWallet,
    override val networks: List<Network>
) : TwoFactorAuthenticationViewModelAbstract(
    greenWallet = greenWallet
) {
    companion object {
        fun preview() = TwoFactorAuthenticationViewModelPreview(
            previewWallet(), listOf(
                previewNetwork(), previewNetwork()
            )
        )
    }
}