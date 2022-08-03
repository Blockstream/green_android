package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.gdk.data.Account
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemOutputDescriptorsBinding
import com.blockstream.green.gdk.getNetworkIcon
import com.mikepenz.fastadapter.binding.AbstractBindingItem

data class OutputDescriptorListItem constructor(
    val account: Account,
    val isOutputDescriptor: Boolean = true
) : AbstractBindingItem<ListItemOutputDescriptorsBinding>() {
    override val type: Int
        get() = R.id.fastadapter_output_descriptor_item_id

    init {
        identifier = hashCode().toLong()
    }

    override fun bindView(binding: ListItemOutputDescriptorsBinding, payloads: List<Any>) {
        binding.name.text = account.name
        binding.icon.setImageResource(account.network.getNetworkIcon())
        if(isOutputDescriptor){
            binding.content.text = account.outputDescriptors
        }else{
            binding.content.text = account.extendedPubkey
        }
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemOutputDescriptorsBinding =
        ListItemOutputDescriptorsBinding.inflate(inflater, parent, false)
}