package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemActionBinding
import com.blockstream.green.utils.StringHolder
import com.mikepenz.fastadapter.binding.AbstractBindingItem

data class ActionListItem constructor(
    private val title: StringHolder = StringHolder(),
    private val message: StringHolder = StringHolder(),
    var button: StringHolder = StringHolder(),
    var buttonOutline: StringHolder = StringHolder(),
    var buttonText: StringHolder = StringHolder()
) : AbstractBindingItem<ListItemActionBinding>() {
    override val type: Int
        get() = R.id.fastadapter_action_item_id

    init {
        identifier = hashCode().toLong()
    }

    override fun bindView(binding: ListItemActionBinding, payloads: List<Any>) {
        title.applyToOrHide(binding.title)
        message.applyToOrHide(binding.message)
        button.applyToOrHide(binding.button)
        buttonOutline.applyToOrHide(binding.buttonOutline)
        buttonText.applyToOrHide(binding.buttonText)
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemActionBinding {
        return ListItemActionBinding.inflate(inflater, parent, false)
    }
}