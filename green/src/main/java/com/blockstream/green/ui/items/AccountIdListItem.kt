package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAccountIdBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem


class AccountIdListItem constructor(val listener : View.OnClickListener) : AbstractBindingItem<ListItemAccountIdBinding>() {
    override val type: Int
        get() = R.id.fastadapter_account_id_item_id

    init {
        identifier = javaClass.hashCode().toLong()
    }

    override fun bindView(binding: ListItemAccountIdBinding, payloads: List<Any>) {
        binding.buttonAccountId.setOnClickListener(listener)
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemAccountIdBinding {
        return ListItemAccountIdBinding.inflate(inflater, parent, false)
    }
}