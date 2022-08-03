package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.blockstream.green.R
import com.blockstream.green.database.Wallet
import com.blockstream.green.databinding.ListItemWalletBinding
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.gdk.iconResource
import com.mikepenz.fastadapter.binding.AbstractBindingItem

data class WalletListItem constructor(val wallet: Wallet, val session: GdkSession) : AbstractBindingItem<ListItemWalletBinding>() {
    override val type: Int
        get() = R.id.fastadapter_wallet_item_id

    init {
        identifier = wallet.id
    }

    override fun bindView(binding: ListItemWalletBinding, payloads: List<Any>) {
        binding.wallet = wallet
        binding.session = session
        binding.connectionIcon.isVisible = session.isConnected
        binding.device = session.device

        binding.icon.setImageResource(wallet.iconResource(session))
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemWalletBinding {
        return ListItemWalletBinding.inflate(inflater, parent, false)
    }
}