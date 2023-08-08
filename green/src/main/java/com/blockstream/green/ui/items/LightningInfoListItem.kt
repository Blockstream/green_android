package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import breez_sdk.NodeState
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemLightningInfoBinding
import com.blockstream.green.extensions.context
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.utils.toAmountLook
import com.blockstream.lightning.inboundLiquiditySatoshi
import com.blockstream.lightning.onchainBalanceSatoshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LightningInfoListItem constructor(
    val session: GdkSession,
    val nodeState: NodeState,
) : AbstractBindingItem<ListItemLightningInfoBinding>() {
    override val type: Int
        get() = R.id.fastadapter_lightning_info_item_id

    override fun createScope(): CoroutineScope = session.createScope(Dispatchers.Main)

    init {
        identifier = "LightningInfoListItem".hashCode().toLong()
    }

    override fun bindView(binding: ListItemLightningInfoBinding, payloads: List<Any>) {

        scope.launch {
            binding.sweepText = if (nodeState.onchainBalanceSatoshi() > 0) {
                binding.context().getString(
                    R.string.id_you_can_sweep_s_of_your_funds,
                    withContext(context = Dispatchers.IO) {
                        nodeState.onchainBalanceSatoshi().toAmountLook(
                            session = session
                        )
                    }
                )
            } else {
                null
            }

            binding.capacityText = binding.context().getString(
                R.string.id_your_current_receive_capacity,
                withContext(context = Dispatchers.IO) {
                    nodeState.inboundLiquiditySatoshi().toAmountLook(
                        session = session
                    )
                }
            )
        }
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemLightningInfoBinding {
        return ListItemLightningInfoBinding.inflate(inflater, parent, false)
    }
}
