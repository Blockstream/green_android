package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.size
import com.blockstream.green.R
import com.blockstream.green.gdk.GreenSession
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.databinding.ListItemTransactionAssetBinding
import com.blockstream.green.databinding.ListItemTransactionBinding
import com.blockstream.green.ui.looks.TransactionListLook
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import mu.KLogging

data class TransactionListItem constructor(val session: GreenSession, val tx: Transaction) : AbstractBindingItem<ListItemTransactionBinding>() {
    override val type: Int
        get() = R.id.fastadapter_transaction_item_id

    private val look : TransactionListLook

    init{
        identifier = tx.txHash.hashCode().toLong()
        look = TransactionListLook(session, tx)
    }

    private fun setAsset(index: Int, binding: ListItemTransactionAssetBinding){
        binding.value.text = look.amount(index)
        binding.value.setTextColor(ContextCompat.getColor(binding.value.context, look.valueColor))
        binding.ticker.text = look.ticker(index)
        binding.icon.setImageDrawable(look.getIcon(index, binding.icon.context))
    }

    override fun bindView(binding: ListItemTransactionBinding, payloads: List<Any>) {
        binding.isLoading = tx.blockHeight == -1L

        if(binding.isLoading == true){ return }

        binding.confirmations = tx.getConfirmations(session.blockHeight).also { logger.info { "Confs: $it" } }
        binding.date = look.date
        binding.memo = look.memo

        setAsset(0, binding.firstValue)

        // remove all view other than main value
        while (binding.assetWrapper.size > 1){
            binding.assetWrapper.removeViewAt(1)
        }

        for(i in 1 until look.assetSize){
            val assetBinding = ListItemTransactionAssetBinding.inflate(LayoutInflater.from(binding.root.context))
            setAsset(i, assetBinding)
            binding.assetWrapper.addView(assetBinding.root)
        }
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemTransactionBinding {
        return ListItemTransactionBinding.inflate(inflater, parent, false)
    }

    companion object: KLogging()
}