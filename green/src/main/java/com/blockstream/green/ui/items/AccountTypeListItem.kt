package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.gdk.data.AccountType
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAccountTypeBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

data class AccountTypeListItem constructor(
    val accountType: AccountType
) :
    AbstractBindingItem<ListItemAccountTypeBinding>() {
    override val type: Int
        get() = R.id.fastadapter_account_type_item_id

    init {
        identifier = accountType.ordinal.toLong()
    }

    override fun bindView(binding: ListItemAccountTypeBinding, payloads: List<Any>) {
        binding.accountType = accountType
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemAccountTypeBinding {
        return ListItemAccountTypeBinding.inflate(inflater, parent, false)
    }
}