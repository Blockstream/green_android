package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.size
import com.blockstream.green.R
import com.blockstream.green.gdk.GreenSession
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.databinding.ListItemTransactionAssetBinding
import com.blockstream.green.databinding.ListItemTransactionBinding
import com.blockstream.green.ui.looks.TransactionListLook
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import mu.KLogging

// Confirmation is used as part of the data class so that we can identify if the item needs to be re-draw
// based on equal() method of the implemented data class
data class TransactionListItem constructor(
    val session: GreenSession,
    val tx: Transaction,
    val confirmations: Int
) : AbstractBindingItem<ListItemTransactionBinding>() {

    override val type: Int
        get() = R.id.fastadapter_transaction_item_id

    private val look: TransactionListLook

    init {
        identifier = tx.txHash.hashCode().toLong()
        look = TransactionListLook(session, tx)
    }

    override fun bindView(binding: ListItemTransactionBinding, payloads: List<Any>) {
        if (tx.isLoadingTransaction()) {
            binding.isLoading = true
            return
        }

        binding.isLoading = false
        binding.confirmations = confirmations
        binding.confirmationsRequired = session.network.confirmationsRequired
        binding.date = look.date
        binding.memo = look.memo

        look.setAssetToBinding(0, binding.firstValue)

        // remove all view other than main value
        while (binding.assetWrapper.size > 1) {
            binding.assetWrapper.removeViewAt(1)
        }

        for (i in 1 until look.assetSize) {
            val assetBinding =
                ListItemTransactionAssetBinding.inflate(LayoutInflater.from(binding.root.context))
            look.setAssetToBinding(i, assetBinding)
            binding.assetWrapper.addView(assetBinding.root)
        }
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemTransactionBinding {
        return ListItemTransactionBinding.inflate(inflater, parent, false)
    }

    companion object : KLogging()
}