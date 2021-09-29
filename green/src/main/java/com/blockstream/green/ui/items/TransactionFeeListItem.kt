package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemTitleBinding
import com.blockstream.green.databinding.ListItemTransactionFeeBinding
import com.blockstream.green.ui.looks.TransactionLook
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.ui.utils.StringHolder

data class TransactionFeeListItem constructor(
    val tx: Transaction,
    val look: TransactionLook
) : AbstractBindingItem<ListItemTransactionFeeBinding>() {
    override val type: Int
        get() = R.id.fastadapter_transaction_fee_item_id

    init {
        identifier = tx.fee
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