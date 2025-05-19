package com.blockstream.common.looks.wallet

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_hardware_wallet
import blockstream_green.common.generated.resources.id_mobile_wallet
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.WalletIcon
import com.blockstream.common.extensions.previewWalletListView
import com.blockstream.common.managers.SessionManager
import org.jetbrains.compose.resources.getString

data class WalletListLook constructor(
    val greenWallet: GreenWallet,
    val title: String,
    val subtitle: String,
    val isConnected: Boolean,
    val isLightningShortcutConnected: Boolean,
    val icon: WalletIcon,
) {

    companion object {

        suspend fun create(wallet: GreenWallet, sessionManager: SessionManager): WalletListLook {
            val session = sessionManager.getWalletSessionOrCreate(wallet)
            val lightningShortcutSession =
                sessionManager.getWalletSessionOrNull(wallet.lightningShortcutWallet())

            return WalletListLook(
                greenWallet = wallet,
                title = wallet.name,
                subtitle = when {
                    wallet.isEphemeral -> session.device?.name ?: wallet.ephemeralBip39Name
                    wallet.isHardware -> getString(Res.string.id_hardware_wallet)
                    else -> getString(Res.string.id_mobile_wallet)
                },
                isConnected = session.isConnected,
                isLightningShortcutConnected = lightningShortcutSession?.isConnected == true,
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