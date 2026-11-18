package com.blockstream.compose.models.settings

import com.blockstream.compose.utils.toStringHolder
import com.blockstream.compose.utils.StringHolder
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_2fa_methods
import com.blockstream.data.data.GreenWallet
import com.blockstream.compose.extensions.previewNetwork
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.data.gdk.data.Network
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavData

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
        _navData.value = NavData(
            title = StringHolder(stringResource = Res.string.id_2fa_methods),
            subtitle = greenWallet.name.toStringHolder()
        )

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