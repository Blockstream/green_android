package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemHelpBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.ui.utils.StringHolder

class HelpListItem(
    private val title: StringHolder = StringHolder(null),
    private val message: StringHolder = StringHolder(null),
    private val buttonText: StringHolder = StringHolder(null)
) :
    AbstractBindingItem<ListItemHelpBinding>() {
    override val type: Int
        get() = R.id.fastadapter_help_item_id

    override fun bindView(binding: ListItemHelpBinding, payloads: List<Any>) {
        title.applyToOrHide(binding.title)
        message.applyToOrHide(binding.message)
        buttonText.applyToOrHide(binding.button)
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemHelpBinding {
        return ListItemHelpBinding.inflate(inflater, parent, false)
    }
}