package com.blockstream.green.ui.items

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.gdk.data.Network
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemBlockHeaderBinding
import com.blockstream.green.utils.StringHolder
import com.mikepenz.fastadapter.binding.AbstractBindingItem

data class BlockHeaderListItem constructor(val height: StringHolder, val network: Network) : AbstractBindingItem<ListItemBlockHeaderBinding>() {
    override val type: Int
        get() = R.id.fastadapter_block_header_item_id

    init {
        identifier = 0L
    }

    @SuppressLint("SetTextI18n")
    override fun bindView(binding: ListItemBlockHeaderBinding, payloads: List<Any>) {
        binding.network.text = network.productName
        height.applyTo(binding.height)
        binding.icon.setImageResource(network.getNetworkIcon())
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemBlockHeaderBinding {
        return ListItemBlockHeaderBinding.inflate(inflater, parent, false)
    }
}