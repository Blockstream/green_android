package com.blockstream.green.ui.items

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemMenuBinding
import com.blockstream.green.utils.StringHolder
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import kotlinx.parcelize.Parcelize

@Parcelize
data class MenuListItem constructor(
    val icon: Int = 0,
    val title: StringHolder
) : AbstractBindingItem<ListItemMenuBinding>(), Parcelable {
    override val type: Int
        get() = R.id.fastadapter_menu_item_id

    init {
        identifier = title.hashCode().toLong()
    }

    override fun bindView(binding: ListItemMenuBinding, payloads: List<Any>) {
        title.applyTo(binding.title)
        if(icon > 0) {
            binding.icon.setImageResource(icon)
            binding.icon.isVisible = true
        }else{
            binding.icon.isVisible = false
        }
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemMenuBinding {
        return ListItemMenuBinding.inflate(inflater, parent, false)
    }
}