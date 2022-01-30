package com.blockstream.green.ui.items

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemTextBinding
import com.blockstream.green.utils.StringHolder
import com.mikepenz.fastadapter.binding.AbstractBindingItem

data class TextListItem constructor(
    val text: StringHolder,
    val textColor: Int = R.color.color_on_surface_emphasis_high,
    val textAlignment: Int = View.TEXT_ALIGNMENT_TEXT_START,
    val textTypeface: Int = Typeface.NORMAL,
    val paddingTop: Int = R.dimen.dp16,
    val paddingBottom: Int = R.dimen.dp16,
    val paddingLeft: Int = R.dimen.dp16,
    val paddingRight: Int = R.dimen.dp16,
) : AbstractBindingItem<ListItemTextBinding>() {
    override val type: Int
        get() = R.id.fastadapter_text_item_id

    init {
        identifier = text.hashCode().toLong()
    }

    override fun bindView(binding: ListItemTextBinding, payloads: List<Any>) {
        val res = binding.root.resources

        text.applyTo(binding.text)
        binding.text.setTextColor(ContextCompat.getColor(binding.root.context, textColor))
        binding.text.textAlignment = textAlignment
        binding.text.setTypeface(binding.text.typeface, textTypeface)

        binding.root.updatePadding(
            top = res.getDimension(paddingTop).toInt(),
            bottom = res.getDimension(paddingBottom).toInt(),
            left = res.getDimension(paddingLeft).toInt(),
            right = res.getDimension(paddingRight).toInt(),
        )
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemTextBinding {
        return ListItemTextBinding.inflate(inflater, parent, false)
    }
}