package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.size
import com.blockstream.green.R
import com.blockstream.green.gdk.GreenSession
import com.blockstream.gdk.data.Transaction
import com.blockstream.green.databinding.ListItemTransactionAssetBinding
import com.blockstream.green.databinding.ListItemTransactionBinding
import com.blockstream.green.ui.looks.TransactionListLook
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class TransactionListItem(val session: GreenSession, val tx: Transaction) : AbstractBindingItem<ListItemTransactionBinding>() {
    override val type: Int
        get() = R.id.fastadapter_transaction_item_id

    private val look : TransactionListLook

    init{
        identifier = tx.hashCode().toLong()
        look = TransactionListLook(session, tx)
    }

    private fun setAsset(index: Int, binding: ListItemTransactionAssetBinding){
        binding.value.text = look.amount(index)
        binding.value.setTextColor(ContextCompat.getColor(binding.value.context, look.valueColor))
        binding.ticker.text = look.ticker(index)
        binding.icon.setImageDrawable(look.getIcon(index, binding.icon.context))
    }

    override fun bindView(binding: ListItemTransactionBinding, payloads: List<Any>) {
        binding.date.text = look.date
        binding.note.text = look.memo

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
}