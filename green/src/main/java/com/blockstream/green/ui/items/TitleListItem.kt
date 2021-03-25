package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemTitleBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.ui.utils.StringHolder

data class TitleListItem(
    val title: StringHolder,
    val showBackButton: Boolean = false,
    val withTopPadding: Boolean = true,
    val withBottomPadding: Boolean = true
) : AbstractBindingItem<ListItemTitleBinding>() {
    override val type: Int
        get() = R.id.fastadapter_title_item_id

    override var identifier: Long
        get() = title.textString?.hashCode()?.toLong() ?: title.textRes.toLong()
        set(value) {}

    override fun bindView(binding: ListItemTitleBinding, payloads: List<Any>) {
        val res = binding.root.resources
        title.applyTo(binding.title)

        binding.root.updatePadding(
            top = res.getDimension(if (withTopPadding) R.dimen.dp16 else R.dimen.dp0).toInt(),
            bottom = res.getDimension(if (withBottomPadding) R.dimen.dp16 else R.dimen.dp0).toInt()
        )
        binding.back.isVisible = showBackButton
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemTitleBinding {
        return ListItemTitleBinding.inflate(inflater, parent, false)
    }
}