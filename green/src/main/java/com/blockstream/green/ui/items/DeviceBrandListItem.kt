package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemDeviceBrandBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class DeviceBrandListItem(val isBluetooth: Boolean) : AbstractBindingItem<ListItemDeviceBrandBinding>() {
    override val type: Int
        get() = R.id.fastadapter_device_brand_item_id

    override fun bindView(binding: ListItemDeviceBrandBinding, payloads: List<Any>) {
        binding.name = binding.root.context.getString(if(isBluetooth) R.string.id_wireless else R.string.id_cable)
        binding.icon.setImageResource(if(isBluetooth) R.drawable.ic_ble else R.drawable.ic_sharp_usb_24)
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemDeviceBrandBinding {
        return ListItemDeviceBrandBinding.inflate(inflater, parent, false)
    }
}