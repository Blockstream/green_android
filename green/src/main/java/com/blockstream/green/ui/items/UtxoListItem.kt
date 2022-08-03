package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.gdk.data.Utxo
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemUtxoBinding
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.extensions.bind
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import kotlinx.coroutines.CoroutineScope


data class UtxoListItem constructor(
    val scope: CoroutineScope,
    val utxo: Utxo,
    val session: GdkSession,
    val showHash: Boolean = false,
    val showName: Boolean = true,
    val isMultiselection: Boolean = false
) :
    AbstractBindingItem<ListItemUtxoBinding>() {
    override val type: Int
        get() = R.id.fastadapter_utxo_item_id

    init {
        identifier = utxo.hashCode().toLong()
    }

    override fun bindView(binding: ListItemUtxoBinding, payloads: List<Any>) {

        binding.utxo.bind(scope = scope, utxo = utxo, session = session, showHash = showHash, showName = showName)

        if (isMultiselection) {
            binding.materiaCard.isChecked = false
            binding.root.setOnClickListener {
                binding.materiaCard.isChecked = !binding.materiaCard.isChecked
            }
        }
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemUtxoBinding {
        return ListItemUtxoBinding.inflate(inflater, parent, false)
    }
}
