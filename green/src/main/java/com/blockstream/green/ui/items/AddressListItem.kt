package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.gdk.data.Address
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAddressBinding
import com.blockstream.green.extensions.context
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import mu.KLogging


data class AddressListItem constructor(
    val index: Int,
    val address: Address
) : AbstractBindingItem<ListItemAddressBinding>() {
    override val type: Int
        get() = R.id.fastadapter_address_item_id

    init {
        identifier = "AddressListItem".hashCode() + address.hashCode().toLong()
    }

    override fun bindView(binding: ListItemAddressBinding, payloads: List<Any>) {
        binding.index = "#${index}"
        binding.address = address.address
        binding.txCount = binding.context().getString(R.string.id_tx_count)+ ": " + address.txCount
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemAddressBinding {
        return ListItemAddressBinding.inflate(inflater, parent, false)
    }

    companion object: KLogging()
}