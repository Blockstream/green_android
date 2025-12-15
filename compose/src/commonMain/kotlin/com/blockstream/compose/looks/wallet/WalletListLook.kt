package com.blockstream.compose.looks.wallet

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_hardware_wallet
import blockstream_green.common.generated.resources.id_mobile_wallet
import blockstream_green.common.generated.resources.id_watchonly
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.WalletIcon
import com.blockstream.data.managers.SessionManager
import com.blockstream.compose.extensions.previewWalletListView
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
                isWatchOnly = wallet.isWatchOnly || wallet.isWatchOnlyQr,
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