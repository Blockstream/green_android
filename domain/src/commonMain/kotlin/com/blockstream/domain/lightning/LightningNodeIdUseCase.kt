package com.blockstream.domain.lightning

import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.managers.WalletSettingsManager
import com.blockstream.jade.Loggable

class LightningNodeIdUseCase(private val walletSettingsManager: WalletSettingsManager) : Loggable() {
    suspend operator fun invoke(wallet: GreenWallet, session: GdkSession) {
        val lightningNodeId = session.lightningNodeId

        logger.d { "Lightning NodeId: $lightningNodeId" }

        if (!wallet.isEphemeral && !lightningNodeId.isNullOrBlank()) {
            walletSettingsManager.setLightningNodeId(walletId = wallet.id, nodeId = lightningNodeId)
        }
    }
}