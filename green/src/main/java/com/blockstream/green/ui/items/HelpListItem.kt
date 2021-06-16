package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemHelpBinding
import com.blockstream.green.utils.StringHolder
import com.mikepenz.fastadapter.binding.AbstractBindingItem

data class HelpListItem(
    private val title: StringHolder = StringHolder(),
    private val message: StringHolder = StringHolder(),
    private val buttonText: StringHolder = StringHolder()
) : AbstractBindingItem<ListItemHelpBinding>() {
    override val type: Int
        get() = R.id.fastadapter_help_item_id

    init {
        identifier = hashCode().toLong()
    }

    override fun bindView(binding: ListItemHelpBinding, payloads: List<Any>) {
        title.applyToOrHide(binding.title)
        message.applyToOrHide(binding.message)
        buttonText.applyToOrHide(binding.button)
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemHelpBinding {
        return ListItemHelpBinding.inflate(inflater, parent, false)
    }
}