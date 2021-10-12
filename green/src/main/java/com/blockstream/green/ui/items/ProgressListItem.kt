package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemProgressBinding
import com.blockstream.green.databinding.ListItemTitleBinding
import com.blockstream.green.utils.StringHolder
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class ProgressListItem : AbstractBindingItem<ListItemProgressBinding>() {
    override val type: Int
        get() = R.id.fastadapter_progress_item_id

    init {
        identifier = hashCode().toLong()
    }

    override fun bindView(binding: ListItemProgressBinding, payloads: List<Any>) {

    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemProgressBinding {
        return ListItemProgressBinding.inflate(inflater, parent, false)
    }
}