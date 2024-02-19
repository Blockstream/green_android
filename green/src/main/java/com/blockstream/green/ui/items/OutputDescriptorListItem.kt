package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.common.gdk.data.Account
import com.blockstream.common.looks.wallet.WatchOnlyLook
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemOutputDescriptorsBinding
import com.blockstream.green.gdk.getNetworkIcon
import com.mikepenz.fastadapter.binding.AbstractBindingItem

data class OutputDescriptorListItem constructor(
    val look: WatchOnlyLook
) : AbstractBindingItem<ListItemOutputDescriptorsBinding>() {
    override val type: Int
        get() = R.id.fastadapter_output_descriptor_item_id

    init {
        identifier = hashCode().toLong()
    }

    val account: Account
        get() = look.account!!

    val isOutputDescriptor
        get() = look.outputDescriptors != null

    override fun bindView(binding: ListItemOutputDescriptorsBinding, payloads: List<Any>) {
        binding.name.text = account.name
        binding.icon.setImageResource(account.network.getNetworkIcon())
        if(look.outputDescriptors != null){
            binding.content.text = look.outputDescriptors
        }else{
            binding.content.text = look.extendedPubkey
        }
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemOutputDescriptorsBinding =
        ListItemOutputDescriptorsBinding.inflate(inflater, parent, false)
}