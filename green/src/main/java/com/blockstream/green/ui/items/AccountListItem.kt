package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAccountBinding
import com.blockstream.gdk.data.Network
import com.blockstream.gdk.data.SubAccount
import com.blockstream.green.gdk.titleRes
import com.mikepenz.fastadapter.binding.AbstractBindingItem


class AccountListItem(val subAccount: SubAccount, private val network: Network) : AbstractBindingItem<ListItemAccountBinding>() {
    override val type: Int
        get() = R.id.fastadapter_account_item_id

    init {
        identifier = subAccount.pointer
    }

    override fun bindView(binding: ListItemAccountBinding, payloads: List<Any>) {
        binding.card.setCardBackgroundColor(ContextCompat.getColor(binding.card.context, if(network.isLiquid) R.color.liquid else if(network.isMainnet) R.color.bitcoin else R.color.testnet))
        binding.title.text = subAccount.nameOrDefault(binding.root.resources.getString(R.string.id_main_account))
        binding.type.setText(subAccount.type.titleRes())
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemAccountBinding {
        return ListItemAccountBinding.inflate(inflater, parent, false)
    }
}