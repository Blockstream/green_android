package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemTransactionHashBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import mu.KLogging

data class TransactionHashListItem constructor(
    val transaction: Transaction
) : AbstractBindingItem<ListItemTransactionHashBinding>() {
    override val type: Int
        get() = R.id.fastadapter_transaction_hash_item_id

    init {
        identifier = "TransactionHashListItem".hashCode().toLong()
    }

    override fun bindView(binding: ListItemTransactionHashBinding, payloads: List<Any>) {
        binding.hash = transaction.txHash
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemTransactionHashBinding {
        return ListItemTransactionHashBinding.inflate(inflater, parent, false)
    }

    companion object : KLogging()
}