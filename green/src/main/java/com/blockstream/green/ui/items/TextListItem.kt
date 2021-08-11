package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemTextBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.ui.utils.StringHolder

data class TextListItem(
    val title: StringHolder,
) : AbstractBindingItem<ListItemTextBinding>() {
    override val type: Int
        get() = R.id.fastadapter_text_item_id

    init {
        identifier = title.textString?.hashCode()?.toLong() ?: title.textRes.toLong()
    }

    override fun bindView(binding: ListItemTextBinding, payloads: List<Any>) {
        title.applyTo(binding.title)
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemTextBinding {
        return ListItemTextBinding.inflate(inflater, parent, false)
    }
}