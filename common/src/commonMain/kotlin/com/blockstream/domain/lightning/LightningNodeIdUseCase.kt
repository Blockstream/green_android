package com.blockstream.domain.lightning

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.managers.WalletSettingsManager
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