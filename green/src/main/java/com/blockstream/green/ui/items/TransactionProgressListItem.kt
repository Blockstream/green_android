package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.views.TransactionStatusLook
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemTransactionProgressBinding
import com.blockstream.green.extensions.context
import com.blockstream.green.extensions.createdAt
import com.blockstream.green.extensions.resource
import com.blockstream.green.extensions.stringFromIdentifierOrNull
import com.blockstream.green.utils.formatMediumWithTime
import com.mikepenz.fastadapter.binding.AbstractBindingItem


data class TransactionProgressListItem constructor(
    private val transactionStatusLook: TransactionStatusLook,
    private val transaction: Transaction,
) : AbstractBindingItem<ListItemTransactionProgressBinding>() {
    override val type: Int
        get() = R.id.fastadapter_transaction_progress_item_id

    init {
        identifier = "TransactionProgressListItem".hashCode().toLong()
    }

    override fun bindView(binding: ListItemTransactionProgressBinding, payloads: List<Any>) {
        binding.tx = transaction
        binding.date = if(transaction.createdAtTs == 0L) "-" else transaction.createdAt().formatMediumWithTime()
        binding.confirmations = transactionStatusLook.confirmations
        binding.confirmationsRequired = transactionStatusLook.confirmationsRequired
        binding.canRBF = transactionStatusLook.canRBF
        binding.isRefundableSwap = transactionStatusLook.isRefundableSwap

        val spv = transaction.spv

        binding.spvEnabled = spv != Transaction.SPVResult.Disabled
        binding.spvInProgress = spv.inProgressOrUnconfirmed()
        binding.spvFailed = spv.failed()

        binding.status.text = binding.context().stringFromIdentifierOrNull(transactionStatusLook.statusText)

        binding.status.setTextColor(
            ContextCompat.getColor(
                binding.root.context, transactionStatusLook.statusColor.resource()
            )
        )

        if (transaction.spv != Transaction.SPVResult.Disabled) {
            binding.spv.setImageResource(
                when (transaction.spv) {
                    Transaction.SPVResult.InProgress, Transaction.SPVResult.Unconfirmed -> R.drawable.ic_spv_in_progress
                    Transaction.SPVResult.NotLongest -> R.drawable.ic_spv_warning
                    Transaction.SPVResult.Verified -> R.drawable.ic_spv_verified
                    else -> R.drawable.ic_spv_error
                }
            )
        }
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemTransactionProgressBinding {
        return ListItemTransactionProgressBinding.inflate(inflater, parent, false)
    }
}