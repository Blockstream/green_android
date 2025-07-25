package com.blockstream.common.looks.wallet

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_hardware_wallet
import blockstream_green.common.generated.resources.id_mobile_wallet
import blockstream_green.common.generated.resources.id_watchonly
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.WalletIcon
import com.blockstream.common.extensions.previewWalletListView
import com.blockstream.common.managers.SessionManager
import org.jetbrains.compose.resources.getString

data class WalletListLook constructor(
    val greenWallet: GreenWallet,
    val title: String,
    val subtitle: String,
    val isWatchOnly: Boolean,
    val isConnected: Boolean,
    val icon: WalletIcon,
) {

    companion object {

        suspend fun create(wallet: GreenWallet, sessionManager: SessionManager): WalletListLook {
            val session = sessionManager.getWalletSessionOrCreate(wallet)

            return WalletListLook(
                greenWallet = wallet,
                title = wallet.name,
                subtitle = when {
                    wallet.isEphemeral -> session.device?.name ?: wallet.ephemeralBip39Name
                    wallet.isHardware -> getString(Res.string.id_hardware_wallet)
                    wallet.isWatchOnly -> getString(Res.string.id_watchonly)
                    else -> getString(Res.string.id_mobile_wallet)
                },
                isWatchOnly = wallet.isWatchOnly,
                isConnected = session.isConnected,
                icon = wallet.icon
            )
        }

        fun preview(
            isHardware: Boolean = false,
            isEphemeral: Boolean = false,
            isConnected: Boolean = false,
        ): WalletListLook {
            return previewWalletListView(
                isHardware = isHardware,
                isEphemeral = isEphemeral,
                isConnected = isConnected
            )
        }
    }
}