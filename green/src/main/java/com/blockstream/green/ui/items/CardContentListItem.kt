package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemCardContentBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class CardContentListItem(val title: String) : AbstractBindingItem<ListItemCardContentBinding>() {
    override val type: Int
        get() = R.id.fastadapter_card_content_item_id

    override fun bindView(binding: ListItemCardContentBinding, payloads: List<Any>) {
        binding.title.text = title
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemCardContentBinding {
        return ListItemCardContentBinding.inflate(inflater, parent, false)
    }
}