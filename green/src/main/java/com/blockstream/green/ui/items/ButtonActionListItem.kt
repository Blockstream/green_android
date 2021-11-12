package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemButtonActionBinding
import com.blockstream.green.utils.StringHolder
import com.mikepenz.fastadapter.binding.AbstractBindingItem


data class ButtonActionListItem constructor(
    private val text: StringHolder,
    private val useCard: Boolean,
    private val extraPadding: Boolean
) : AbstractBindingItem<ListItemButtonActionBinding>() {
    override val type: Int
        get() = R.id.fastadapter_button_action_item_id

    init {
        identifier = hashCode().toLong()
    }

    override fun bindView(binding: ListItemButtonActionBinding, payloads: List<Any>) {

        val padding =
            binding.root.resources.getDimension(if (extraPadding) R.dimen.dp32 else R.dimen.dp16)
                .toInt()
        binding.root.updatePadding(left = padding, right = padding)

        text.applyTo(binding.textView)
        text.applyTo(binding.button)

        binding.button.isVisible = !useCard
        binding.textView.isVisible = useCard
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemButtonActionBinding {
        return ListItemButtonActionBinding.inflate(inflater, parent, false)
    }
}