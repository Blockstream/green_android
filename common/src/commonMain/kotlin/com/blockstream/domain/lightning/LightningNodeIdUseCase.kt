package com.blockstream.domain.lightning

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.WalletExtras
import com.blockstream.common.database.Database
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.gdk.GdkSession
import com.blockstream.jade.Loggable

class LightningNodeIdUseCase(private val database: Database) : Loggable() {
    suspend operator fun invoke(wallet: GreenWallet, session: GdkSession) {
        val lightningNodeId = session.lightningNodeId

        logger.d { "Lightning NodeId: $lightningNodeId" }

        if (!wallet.isEphemeral && lightningNodeId.isNotBlank() && wallet.extras?.lightningNodeId != lightningNodeId) {
            wallet.extras = wallet.extras?.copy(lightningNodeId = lightningNodeId) ?: WalletExtras(lightningNodeId = lightningNodeId)
            database.updateWallet(wallet)
        }
    }
}