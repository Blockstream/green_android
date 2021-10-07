package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.databinding.ListItemTransactionAmountBinding
import com.blockstream.green.ui.looks.TransactionDetailsLook
import com.blockstream.green.ui.looks.TransactionListLook
import com.blockstream.green.ui.looks.TransactionLook
import com.mikepenz.fastadapter.binding.AbstractBindingItem


data class TransactionAmountListItem constructor(
    val tx: Transaction,
    val index: Int,
    val look: TransactionDetailsLook
) : AbstractBindingItem<ListItemTransactionAmountBinding>() {
    override val type: Int
        get() = R.id.fastadapter_asset_item_id

    init {
        identifier = index.toLong()
    }

    override fun bindView(binding: ListItemTransactionAmountBinding, payloads: List<Any>) {
        binding.type = tx.txType
        // GDK returns non-confidential addresses for Liquid. Hide them for now
        binding.address = if(look.session.isLiquid) null else tx.addressees.getOrNull(index)

        look.setAssetToBinding(index, binding.amount)

        // disable the animation on some assets
        binding.root.isClickable = !look.isPolicyAsset(index)
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemTransactionAmountBinding {
        return ListItemTransactionAmountBinding.inflate(inflater, parent, false)
    }
}
