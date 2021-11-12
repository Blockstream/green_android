package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemOverlineTextBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.ui.utils.StringHolder

data class OverlineTextListItem constructor(
    var overline: StringHolder,
    var text: StringHolder
) : AbstractBindingItem<ListItemOverlineTextBinding>() {
    override val type: Int
        get() = R.id.fastadapter_overline_text_item_id

    init {
        identifier = hashCode().toLong()
    }

    override fun bindView(binding: ListItemOverlineTextBinding, payloads: List<Any>) {
        overline.applyTo(binding.overline)
        text.applyTo(binding.text)
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemOverlineTextBinding {
        return ListItemOverlineTextBinding.inflate(inflater, parent, false)
    }
}