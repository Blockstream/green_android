package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemRecoveryPhraseWordBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem


data class RecoveryPhraseWordListItem constructor(
    val index: Int,
    val word: String,
    val isActive: Boolean
) : AbstractBindingItem<ListItemRecoveryPhraseWordBinding>() {
    override val type: Int
        get() = R.id.fastadapter_recovery_phrase_word_item_id

    init {
        identifier = index.toLong()
    }

    override fun bindView(binding: ListItemRecoveryPhraseWordBinding, payloads: List<Any>) {
        binding.counter.text = index.toString()
        binding.word.text = word
        binding.word.setBackgroundResource(if(isActive) R.drawable.color_surface_round_stroked else R.drawable.color_surface_round)
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemRecoveryPhraseWordBinding {
        return ListItemRecoveryPhraseWordBinding.inflate(inflater, parent, false)
    }
}