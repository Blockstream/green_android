package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.common.views.account.AddressLook
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAddressBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import mu.KLogging


data class AddressListItem constructor(
    val addressLook: AddressLook
) : AbstractBindingItem<ListItemAddressBinding>() {
    override val type: Int
        get() = R.id.fastadapter_address_item_id

    init {
        identifier = "AddressListItem".hashCode() + addressLook.hashCode().toLong()
    }

    override fun bindView(binding: ListItemAddressBinding, payloads: List<Any>) {
        binding.address = addressLook.address.address
        binding.txCount = addressLook.txCount
        binding.canSign = addressLook.canSign
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemAddressBinding {
        return ListItemAddressBinding.inflate(inflater, parent, false)
    }

    companion object: KLogging()
}