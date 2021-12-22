package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.gdk.data.AccountType
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemContentCardBinding
import com.blockstream.green.gdk.descriptionRes
import com.blockstream.green.gdk.titleRes
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.ui.utils.StringHolder

class AccountTypeListItem constructor(
    val accountType: AccountType
) :
    AbstractBindingItem<ListItemContentCardBinding>() {
    override val type: Int
        get() = R.id.fastadapter_account_type_item_id

    init {
        identifier = accountType.ordinal.toLong()
    }

    override fun bindView(binding: ListItemContentCardBinding, payloads: List<Any>) {
        binding.card.setTitle(StringHolder(accountType.titleRes()).getText(binding.root.context))
        binding.card.setCaption(StringHolder(accountType.descriptionRes()).getText(binding.root.context))
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemContentCardBinding {
        return ListItemContentCardBinding.inflate(inflater, parent, false)
    }
}