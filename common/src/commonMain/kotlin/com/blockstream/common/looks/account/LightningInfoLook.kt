package com.blockstream.common.looks.account

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_lightning_channel_closed_you_can_sweep
import blockstream_green.common.generated.resources.id_your_current_receive_capacity
import breez_sdk.NodeState
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.lightning.isLoading
import com.blockstream.common.lightning.onchainBalanceSatoshi
import com.blockstream.common.lightning.totalInboundLiquiditySatoshi
import com.blockstream.common.utils.toAmountLook
import org.jetbrains.compose.resources.getString

data class LightningInfoLook constructor(val sweep: String? = null, val capacity: String? = null) {

    companion object {
        suspend fun create(session: GdkSession, nodeState: NodeState): LightningInfoLook? {
            if (nodeState.isLoading() || (nodeState.onchainBalanceSatoshi() == 0L && nodeState.totalInboundLiquiditySatoshi() == 0L)) {
                return null
            }

            val sweep =
                if (session.isLightningShortcut || nodeState.onchainBalanceSatoshi() == 0L) null else {
                    getString(
                        Res.string.id_lightning_channel_closed_you_can_sweep,
                        nodeState.onchainBalanceSatoshi().toAmountLook(session = session) ?: ""
                    )
                }

            val capacity = if (nodeState.totalInboundLiquiditySatoshi() == 0L) null else {
                getString(
                    Res.string.id_your_current_receive_capacity,
                    nodeState.totalInboundLiquiditySatoshi().toAmountLook(session = session) ?: ""
                )
            }

            return LightningInfoLook(sweep = sweep, capacity = capacity)
        }
    }
}