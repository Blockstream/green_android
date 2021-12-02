package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemTransactionFeeBinding
import com.blockstream.green.ui.looks.FeeLookInterface
import com.mikepenz.fastadapter.binding.AbstractBindingItem

data class TransactionFeeListItem constructor(
    val look: FeeLookInterface
) : AbstractBindingItem<ListItemTransactionFeeBinding>() {
    override val type: Int
        get() = R.id.fastadapter_transaction_fee_item_id

    init {
        identifier = "TransactionFeeListItem".hashCode().toLong()
    }

    override fun bindView(binding: ListItemTransactionFeeBinding, payloads: List<Any>) {
        binding.fee = look.fee
        binding.feeFiat = look.feeFiat
        binding.feeRate = look.feeRate
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemTransactionFeeBinding {
        return ListItemTransactionFeeBinding.inflate(inflater, parent, false)
    }
}