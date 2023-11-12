package com.blockstream.common.models.settings

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.models.GreenViewModel

class TwoFactorAuthenticationViewModel(
    greenWalletOrNull: GreenWallet,
) : GreenViewModel(
    greenWalletOrNull = greenWalletOrNull
) {
    override fun screenName(): String = "WalletSettings2FA"

    init {
        bootstrap()
    }
}