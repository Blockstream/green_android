package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemButtonActionBinding
import com.blockstream.green.databinding.ListItemTransactionProgressBinding
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.formatAuto
import com.blockstream.green.utils.formatWithTime
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Integer.min


data class TransactionProgressListItem constructor(
    private val transaction: Transaction,
    var confirmations: Int,
    private val confirmationsRequired: Int
) : AbstractBindingItem<ListItemTransactionProgressBinding>() {
    override val type: Int
        get() = R.id.fastadapter_transaction_progress_item_id

    init {
        identifier = transaction.txHash.hashCode().toLong()
    }

    override fun bindView(binding: ListItemTransactionProgressBinding, payloads: List<Any>) {
        binding.date = transaction.createdAt.formatWithTime()
        binding.confirmations = confirmations
        binding.confirmationsRequired = confirmationsRequired
        binding.canRBF = transaction.canRBF
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemTransactionProgressBinding {
        return ListItemTransactionProgressBinding.inflate(inflater, parent, false)
    }
}