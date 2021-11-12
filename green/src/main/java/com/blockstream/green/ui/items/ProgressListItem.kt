package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemProgressBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import kotlin.random.Random

class ProgressListItem : AbstractBindingItem<ListItemProgressBinding>() {
    override val type: Int
        get() = R.id.fastadapter_progress_item_id

    init {
        identifier = Random.nextLong()
    }

    override fun bindView(binding: ListItemProgressBinding, payloads: List<Any>) {

    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemProgressBinding {
        return ListItemProgressBinding.inflate(inflater, parent, false)
    }
}