package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.DeviceBrand
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemDeviceBrandBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class DeviceBrandListItem(val deviceBrand: DeviceBrand) : AbstractBindingItem<ListItemDeviceBrandBinding>() {
    override val type: Int
        get() = R.id.fastadapter_device_brand_item_id

    override fun bindView(binding: ListItemDeviceBrandBinding, payloads: List<Any>) {
        binding.name = deviceBrand.brand
        binding.icon.setImageResource(deviceBrand.icon)
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemDeviceBrandBinding {
        return ListItemDeviceBrandBinding.inflate(inflater, parent, false)
    }
}