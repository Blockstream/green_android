package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAccountBinding
import com.blockstream.gdk.data.Network
import com.blockstream.gdk.data.SubAccount
import com.blockstream.green.gdk.titleRes
import com.mikepenz.fastadapter.binding.AbstractBindingItem


data class AccountListItem constructor(var subAccount: SubAccount, val network: Network,
                                       val isTopAccount: Boolean = false,
                                       var showFakeCard: Boolean = false,
                                       var isAccountListOpen : Boolean = false) :
    AbstractBindingItem<ListItemAccountBinding>() {
    override val type: Int
        get() = R.id.fastadapter_account_item_id

    init {
        identifier = javaClass.hashCode() + subAccount.pointer
    }

    override fun bindView(binding: ListItemAccountBinding, payloads: List<Any>) {
        binding.isTopAccount = isTopAccount
        binding.isAccountListOpen = isAccountListOpen
        binding.isLiquid = network.isLiquid
        binding.isMainnet = network.isMainnet
        binding.subAccount = subAccount

        if(isTopAccount){
            binding.fakeAccountCard.isInvisible = !showFakeCard || isAccountListOpen
        }else{
            binding.fakeAccountCard.isVisible = false
        }
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemAccountBinding {
        return ListItemAccountBinding.inflate(inflater, parent, false)
    }
}