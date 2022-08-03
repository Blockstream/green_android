package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.gdk.data.Account
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAccountWarningBinding
import com.blockstream.green.extensions.context
import com.blockstream.green.utils.greenText
import com.mikepenz.fastadapter.binding.AbstractBindingItem

data class AccountWarningListItem constructor(
    val account: Account,
    val style : Int = 0,
) : AbstractBindingItem<ListItemAccountWarningBinding>() {
    override val type: Int
        get() = R.id.fastadapter_account_warning_item_id

    init {
        identifier = account.id.hashCode().toLong()
    }

    override fun bindView(binding: ListItemAccountWarningBinding, payloads: List<Any>) {
        binding.account = account
        binding.style = style

        if(style == 0){
            binding.textView.text = if(account.isSinglesig){
                binding.context().greenText(R.string.id_you_have_a_significant_amount)
            }else {
                binding.context().greenText(R.string.id_increase_the_security_of_your, R.string.id_adding_a_2fa)
            }
        }
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemAccountWarningBinding {
        return ListItemAccountWarningBinding.inflate(inflater, parent, false)
    }
}