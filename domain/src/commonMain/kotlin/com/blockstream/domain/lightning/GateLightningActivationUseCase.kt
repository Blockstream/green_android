// Gates Lightning activation on Greenlight's capacity answer for the wallet's node, before the node is registered.
package com.blockstream.domain.lightning

import com.blockstream.data.data.GreenWallet
import com.blockstream.data.extensions.tryCatchNullSuspend
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.lightning.GreenlightSdk
import com.blockstream.data.managers.WalletSettingsManager
import com.blockstream.data.utils.toHex

class LightningAtCapacityException : Exception("Lightning is at capacity")

class LightningAvailabilityUnknownException : Exception("Lightning availability is unknown")

class GateLightningActivationUseCase(
    private val walletSettingsManager: WalletSettingsManager,
    private val isLightningAvailableUseCase: IsLightningAvailableUseCase
) {

    // Returns the derived node id so the caller can check it against the node Greenlight registers
    suspend operator fun invoke(session: GdkSession, wallet: GreenWallet, mnemonic: String): String {
        val nodeId = tryCatchNullSuspend { GreenlightSdk.nodeId(mnemonic = mnemonic) }
            ?: throw LightningAvailabilityUnknownException()
        val nodeIdHex = nodeId.toHex()

        if (!wallet.isEphemeral && walletSettingsManager.getLightningAvailableNodeId(wallet.id).equals(nodeIdHex, ignoreCase = true)) {
            return nodeIdHex
        }

        when (isLightningAvailableUseCase(session = session, nodeId = nodeId)) {
            true -> if (!wallet.isEphemeral) {
                walletSettingsManager.setLightningAvailableNodeId(walletId = wallet.id, nodeId = nodeIdHex)
            }

            false -> throw LightningAtCapacityException()
            null -> throw LightningAvailabilityUnknownException()
        }

        return nodeIdHex
    }
}
