package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.size
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemTransactionBinding
import com.blockstream.green.databinding.TransactionAssetLayoutBinding
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.gdk.getConfirmationsMax
import com.blockstream.green.looks.TransactionLook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import mu.KLogging

// Confirmation is used as part of the data class so that we can identify if the item needs to be re-draw
// based on equal() method of the implemented data class
data class TransactionListItem constructor(
    val tx: Transaction,
    val session: GdkSession,
    val showAccount: Boolean = false,
) : AbstractBindingItem<ListItemTransactionBinding>() {

    override val type: Int
        get() = R.id.fastadapter_transaction_item_id

    private val look: TransactionLook

    val confirmations : Int get() = tx.getConfirmationsMax(session)

    override fun createScope(): CoroutineScope {
        return session.createScope(dispatcher = Dispatchers.Main)
    }

    init {
        // Same tx hash can appear on two accounts of the same wallet.
        identifier = tx.txHash.ifBlank { tx }.hashCode().toLong() + tx.accountInjected?.id.hashCode().toLong()
        look = TransactionLook(tx, session)
    }

    override fun bindView(binding: ListItemTransactionBinding, payloads: List<Any>) {

        if (tx.isLoadingTransaction) {
            binding.isLoading = true
            return
        }

        binding.isLoading = false
        binding.type = tx.txType
        binding.confirmations = confirmations
        binding.confirmationsRequired = tx.network.confirmationsRequired
        binding.date = look.date
        binding.memo = look.memo
        binding.account = tx.account.takeIf { showAccount }


        look.setTransactionAssetToBinding(scope, session,0, binding.firstValue)

        // remove all view other than main value
        while (binding.assetWrapper.size > 1) {
            binding.assetWrapper.removeViewAt(1)
        }

        for (i in 1 until look.assetSize) {
            TransactionAssetLayoutBinding.inflate(LayoutInflater.from(binding.root.context)).also {
                binding.assetWrapper.addView(it.root)
                look.setTransactionAssetToBinding(scope, session, i, it)
            }
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