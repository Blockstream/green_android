package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Address
import com.blockstream.common.gdk.data.Network
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAddressBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import mu.KLogging


data class AddressListItem constructor(
    val index: Int,
    val address: Address,
    val network: Network,
    val session: GdkSession
) : AbstractBindingItem<ListItemAddressBinding>() {
    override val type: Int
        get() = R.id.fastadapter_address_item_id

    init {
        identifier = "AddressListItem".hashCode() + address.hashCode().toLong()
    }

    override fun bindView(binding: ListItemAddressBinding, payloads: List<Any>) {
        binding.index = "#${index}"
        binding.address = address.address
        binding.txCount = "${address.txCount ?: 0}"
        binding.canSign = network.canSignMessage
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemAddressBinding {
        return ListItemAddressBinding.inflate(inflater, parent, false)
    }

    companion object: KLogging()
}