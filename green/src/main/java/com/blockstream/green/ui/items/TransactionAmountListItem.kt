package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemTransactionAmountBinding
import com.blockstream.green.ui.looks.AddreseeLookInterface
import com.blockstream.green.ui.looks.TransactionDetailsLook
import com.mikepenz.fastadapter.binding.AbstractBindingItem


data class TransactionAmountListItem constructor(
    val index: Int,
    val look: AddreseeLookInterface
) : AbstractBindingItem<ListItemTransactionAmountBinding>() {
    override val type: Int
        get() = R.id.fastadapter_asset_item_id

    init {
        identifier = index.toLong()
    }

    val assetId: String
        get() = look.getAssetId(index)

    override fun bindView(binding: ListItemTransactionAmountBinding, payloads: List<Any>) {
        binding.isChange = look.isChange(index)
        binding.type = look.txType
        binding.address = look.getAddress(index)
        look.setAssetToBinding(index, binding.amount)

    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemTransactionAmountBinding {
        return ListItemTransactionAmountBinding.inflate(inflater, parent, false)
    }
}
