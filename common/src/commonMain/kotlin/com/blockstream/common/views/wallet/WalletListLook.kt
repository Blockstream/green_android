package com.blockstream.common.views.wallet

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.WalletIcon
import com.blockstream.common.extensions.previewWalletListView
import com.blockstream.common.managers.SessionManager

@Parcelize
data class WalletListLook(
    val greenWallet: GreenWallet,
    val title: String,
    val subtitle: String?,
    val hasLightningShortcut: Boolean,
    val isConnected: Boolean,
    val isLightningShortcutConnected: Boolean,
    val icon: WalletIcon,
) : Parcelable {

    companion object{

        fun create(wallet: GreenWallet, sessionManager: SessionManager): WalletListLook {
            val session = sessionManager.getWalletSessionOrCreate(wallet)
            val lightningShortcutSession = sessionManager.getWalletSessionOrNull(wallet.lightningShortcutWallet())

            return WalletListLook(
                greenWallet = wallet,
                title = wallet.name,
                subtitle = if(wallet.isEphemeral) session.device?.name ?: wallet.ephemeralBip39Name else null,
                hasLightningShortcut = wallet.hasLightningShortcut,
                isConnected = session.isConnected,
                isLightningShortcutConnected = lightningShortcutSession?.isConnected == true,
                icon = wallet.icon
            )
        }

        fun preview(isHardware: Boolean): WalletListLook {
            return previewWalletListView(isHardware)
        }
    }
}