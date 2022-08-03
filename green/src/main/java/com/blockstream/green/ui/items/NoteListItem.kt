package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemTransactionNoteBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import mu.KLogging

data class NoteListItem constructor(
    val note: String
): AbstractBindingItem<ListItemTransactionNoteBinding>() {
    override val type: Int
        get() = R.id.fastadapter_note_item_id

    init {
        identifier = "NoteListItem".hashCode().toLong()
    }

    override fun bindView(binding: ListItemTransactionNoteBinding, payloads: List<Any>) {
        binding.note = note
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemTransactionNoteBinding {
        return ListItemTransactionNoteBinding.inflate(inflater, parent, false)
    }

    companion object: KLogging()
}