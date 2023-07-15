package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.blockstream.common.data.DenominatedValue
import com.blockstream.common.gdk.GdkSession
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemDenominationBinding

data class DenominationListItem constructor(
    val session: GdkSession,
    val denominatedValue: DenominatedValue,
    val isChecked: Boolean = false
) : AbstractBindingItem<ListItemDenominationBinding>() {
    override val type: Int
        get() = R.id.fastadapter_denomination_item_id

    init {
        identifier = denominatedValue.hashCode().toLong()
    }

    override fun bindView(binding: ListItemDenominationBinding, payloads: List<Any>) {
        binding.unit.text = denominatedValue.asNetworkUnit(session)
         denominatedValue.asLook(session).also {
             binding.amount.text = it
             binding.amount.isVisible = !it.isNullOrBlank()
        }
        binding.check.isVisible = isChecked
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemDenominationBinding {
        return ListItemDenominationBinding.inflate(inflater, parent, false)
    }
}