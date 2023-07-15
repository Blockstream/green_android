package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.common.views.wallet.WalletListLook
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemWalletBinding
import com.blockstream.green.gdk.iconResource
import com.mikepenz.fastadapter.binding.AbstractBindingItem

data class WalletListItem constructor(val look: WalletListLook) : AbstractBindingItem<ListItemWalletBinding>() {
    override val type: Int
        get() = R.id.fastadapter_wallet_item_id

    init {
        identifier = look.greenWallet.id.hashCode().toLong()
    }

    override fun bindView(binding: ListItemWalletBinding, payloads: List<Any>) {
        binding.view = look
        binding.icon.setImageResource(look.greenWallet.iconResource())
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemWalletBinding {
        return ListItemWalletBinding.inflate(inflater, parent, false)
    }
}