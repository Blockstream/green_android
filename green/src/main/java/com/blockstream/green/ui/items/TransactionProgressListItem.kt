package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
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
        binding.canRBF = transaction.canRBF && transaction.isIn == false

        val spv = transaction.spv

        binding.spvEnabled = spv != Transaction.SPVResult.Disabled
        binding.spvInProgress = spv.inProgressOrUnconfirmed()
        binding.spvFailed = spv.failed()

        binding.status.setText(
            when {
                confirmations == 0 -> {
                    R.string.id_unconfirmed
                }
                confirmations < confirmationsRequired -> {
                    R.string.id_pending_confirmation
                }
                spv.inProgressOrUnconfirmed() -> {
                    R.string.id_verifying_transactions
                }
                spv == Transaction.SPVResult.NotVerified -> {
                    R.string.id_invalid_merkle_proof
                }
                spv == Transaction.SPVResult.NotLongest -> {
                    R.string.id_not_on_longest_chain
                }
                spv == Transaction.SPVResult.Verified -> {
                    R.string.id_verified
                }
                else -> {
                    R.string.id_completed
                }
            }
        )

        binding.status.setTextColor(
            ContextCompat.getColor(
                binding.root.context,
                if (confirmations < confirmationsRequired || spv.inProgressOrUnconfirmed()) {
                    R.color.color_on_surface_emphasis_low
                } else if (spv == Transaction.SPVResult.NotVerified) {
                    R.color.error
                } else if (spv == Transaction.SPVResult.NotLongest) {
                    R.color.warning
                } else {
                    R.color.brand_green
                }
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