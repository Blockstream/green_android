package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.common.devices.GreenDevice
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemDeviceBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

data class DeviceListItem constructor(val device: GreenDevice) : AbstractBindingItem<ListItemDeviceBinding>() {
    override val type: Int
        get() = R.id.fastadapter_device_item_id

    init {
        identifier = device.connectionIdentifier.hashCode().toLong()
    }

    override fun bindView(binding: ListItemDeviceBinding, payloads: List<Any>) {
        binding.device = device
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemDeviceBinding {
        return ListItemDeviceBinding.inflate(inflater, parent, false)
    }
}