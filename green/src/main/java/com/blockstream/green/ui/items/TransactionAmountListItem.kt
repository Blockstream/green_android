package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemTransactionAmountBinding
import com.blockstream.green.ui.looks.AddreseeLookInterface
import com.blockstream.green.utils.toPixels
import com.mikepenz.fastadapter.binding.AbstractBindingItem


data class TransactionAmountListItem constructor(
    val index: Int,
    val look: AddreseeLookInterface,
    val withStroke : Boolean = false,
) : AbstractBindingItem<ListItemTransactionAmountBinding>() {
    override val type: Int
        get() = R.id.fastadapter_asset_item_id

    init {
        identifier = index.toLong()
    }

    val assetId: String
        get() = look.getAssetId(index)

    override fun bindView(binding: ListItemTransactionAmountBinding, payloads: List<Any>) {
        binding.isChange = look.isChange(index)
        binding.address = look.getAddress(index)
        binding.amount = look.getAmount(index)

        if(withStroke){
            binding.card.strokeWidth = binding.root.context.toPixels(1)
            binding.card.strokeColor = ContextCompat.getColor(binding.root.context, R.color.color_on_surface_divider)
        }
        look.setAssetToBinding(index, binding.amountView)

        // make address,amount & fee selectable
        binding.addressTextView.setTextIsSelectable(true)
        binding.amountView.amountTextView.setTextIsSelectable(true)
        binding.amountView.fiatTextView.setTextIsSelectable(true)
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemTransactionAmountBinding {
        return ListItemTransactionAmountBinding.inflate(inflater, parent, false)
    }
}
