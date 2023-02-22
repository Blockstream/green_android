package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import breez_sdk.NodeState
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemLightningInboundBinding
import com.blockstream.green.extensions.context
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.utils.toAmountLook
import com.blockstream.lightning.inboundLiquiditySatoshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LightningInboundListItem constructor(
    val session: GdkSession,
    val nodeState: NodeState,
) : AbstractBindingItem<ListItemLightningInboundBinding>() {
    override val type: Int
        get() = R.id.fastadapter_lightning_inbound_item_id

    override fun createScope(): CoroutineScope = session.createScope(Dispatchers.Main)

    init {
        identifier = "LightningInboundListItem".hashCode().toLong()
    }

    override fun bindView(binding: ListItemLightningInboundBinding, payloads: List<Any>) {

        scope.launch {
            binding.text = binding.context().getString(
                R.string.id_your_receive_capacity_is_s,
                withContext(context = Dispatchers.IO) {
                    nodeState.inboundLiquiditySatoshi().toAmountLook(
                        session = session
                    )
                }
            )
        }
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemLightningInboundBinding {
        return ListItemLightningInboundBinding.inflate(inflater, parent, false)
    }
}