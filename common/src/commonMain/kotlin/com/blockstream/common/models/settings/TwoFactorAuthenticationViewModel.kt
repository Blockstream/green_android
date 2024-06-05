package com.blockstream.common.models.settings

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_twofactor_authentication
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.models.GreenViewModel
import com.rickclephas.kmp.observableviewmodel.launch
import org.jetbrains.compose.resources.getString

class TwoFactorAuthenticationViewModel(
    greenWalletOrNull: GreenWallet,
) : GreenViewModel(
    greenWalletOrNull = greenWalletOrNull
) {
    override fun screenName(): String = "WalletSettings2FA"

    val networks = listOfNotNull(session.activeBitcoinMultisig, session.activeLiquidMultisig)

    init {
        viewModelScope.launch {
            _navData.value = NavData(title = getString(Res.string.id_twofactor_authentication), subtitle = greenWallet.name)
        }

        bootstrap()
    }
}