package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemTransactionOutputBinding
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.looks.ITransactionLook
import com.blockstream.green.utils.toPixels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

data class TransactionUtxoListItem constructor(
    val index: Int,
    val session: GdkSession,
    val look: ITransactionLook,
    val hiddenAmount: Boolean = false,
    val withStroke : Boolean = false,
) : AbstractBindingItem<ListItemTransactionOutputBinding>() {
    override val type: Int
        get() = R.id.fastadapter_asset_item_id

    init {
        identifier = index.toLong()
    }

    override fun createScope(): CoroutineScope {
        return session.createScope(dispatcher = Dispatchers.Main)
    }

    val txOutput = look.getUtxoView(index)

    override fun bindView(binding: ListItemTransactionOutputBinding, payloads: List<Any>) {
        val utxoView = look.getUtxoView(index)

        binding.isChange = utxoView?.isChange
        binding.address = utxoView?.address
        binding.amount = utxoView?.satoshi

        if(withStroke){
            binding.card.strokeWidth = binding.root.context.toPixels(1)
            binding.card.strokeColor = ContextCompat.getColor(binding.root.context, R.color.color_on_surface_divider)
        }
        look.setTransactionUtxoToBinding(scope, index, binding.amountView)

        // make address,amount & fee selectable
        binding.addressTextView.setTextIsSelectable(true)
        binding.amountView.amountTextView.setTextIsSelectable(true)
        binding.amountView.fiatTextView.setTextIsSelectable(true)
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemTransactionOutputBinding {
        return ListItemTransactionOutputBinding.inflate(inflater, parent, false)
    }
}
