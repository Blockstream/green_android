package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemOverlineTextBinding
import com.blockstream.green.extensions.isNotBlank
import com.blockstream.green.utils.StringHolder
import com.mikepenz.fastadapter.binding.AbstractBindingItem

data class OverlineTextListItem constructor(
    var overline: StringHolder,
    var text: StringHolder,
    val url: String? = null
) : AbstractBindingItem<ListItemOverlineTextBinding>() {
    override val type: Int
        get() = R.id.fastadapter_overline_text_item_id

    init {
        identifier = hashCode().toLong()
    }

    override fun bindView(binding: ListItemOverlineTextBinding, payloads: List<Any>) {
        overline.applyTo(binding.overline)
        text.applyTo(binding.text)
        if(url.isNotBlank()){
            binding.url.text = url
            binding.buttonOpen.isVisible = true
            binding.url.isVisible = true
        }else{
            binding.buttonOpen.isVisible = false
            binding.url.isVisible = false
        }

    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemOverlineTextBinding {
        return ListItemOverlineTextBinding.inflate(inflater, parent, false)
    }
}