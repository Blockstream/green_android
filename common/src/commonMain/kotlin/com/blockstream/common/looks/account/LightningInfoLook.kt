package com.blockstream.common.looks.account

import breez_sdk.NodeState
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.lightning.inboundLiquiditySatoshi
import com.blockstream.common.lightning.isLoading
import com.blockstream.common.lightning.onchainBalanceSatoshi
import com.blockstream.common.utils.toAmountLook


data class LightningInfoLook constructor(val sweep: String? = null, val capacity: String? = null) {

    companion object {
        suspend fun create(session: GdkSession, nodeState: NodeState): LightningInfoLook? {
            if(nodeState.isLoading() || (nodeState.onchainBalanceSatoshi() == 0L && nodeState.inboundLiquiditySatoshi() == 0L)){
                return null
            }

            val sweep = if (session.isLightningShortcut || nodeState.onchainBalanceSatoshi() == 0L) null else {
                    "id_you_can_sweep_s_of_your_funds|${
                        nodeState.onchainBalanceSatoshi().toAmountLook(session = session)
                    }"
                }

            val capacity = if (nodeState.inboundLiquiditySatoshi() == 0L) null else {
                "id_your_current_receive_capacity|${
                    nodeState.inboundLiquiditySatoshi().toAmountLook(
                        session = session
                    )
                }"
            }

            return LightningInfoLook(sweep = sweep, capacity = capacity)
        }
    }
}