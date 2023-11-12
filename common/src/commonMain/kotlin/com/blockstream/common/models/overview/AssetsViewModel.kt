package com.blockstream.common.models.overview

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.models.GreenViewModel

class AssetsViewModel(
    greenWalletOrNull: GreenWallet,
) : GreenViewModel(
    greenWalletOrNull = greenWalletOrNull
) {
    override fun screenName(): String = "Assets"

    init {
        bootstrap()
    }
}