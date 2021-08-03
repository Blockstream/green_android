package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemRecoveryWordBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.ui.utils.StringHolder

data class RecoveryWordListItem(
    val index: Int,
    val word: StringHolder,
) : AbstractBindingItem<ListItemRecoveryWordBinding>() {
    override val type: Int
        get() = R.id.fastadapter_recovery_word_item_id

    init {
        identifier = index.toLong()
    }

    override fun bindView(binding: ListItemRecoveryWordBinding, payloads: List<Any>) {
        binding.counter.text = "$index"
        word.applyTo(binding.word)
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemRecoveryWordBinding {
        return ListItemRecoveryWordBinding.inflate(inflater, parent, false)
    }
}