package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.blockstream.green.R
import com.blockstream.green.database.Wallet
import com.blockstream.green.databinding.ListItemWalletBinding
import com.blockstream.green.gdk.GreenSession
import com.mikepenz.fastadapter.binding.AbstractBindingItem

data class WalletListItem constructor(val wallet: Wallet, val greenSession: GreenSession) : AbstractBindingItem<ListItemWalletBinding>() {
    override val type: Int
        get() = R.id.fastadapter_wallet_item_id

    init {
        identifier = wallet.id
    }

    override fun bindView(binding: ListItemWalletBinding, payloads: List<Any>) {
        binding.wallet = wallet
        binding.innerIcon.setImageResource(if (wallet.isWatchOnly) R.drawable.ic_baseline_visibility_24 else if (wallet.isElectrum) R.drawable.ic_singlesig else R.drawable.ic_multisig)
        binding.connectionIcon.isVisible = greenSession.isConnected

        binding.hardware.isVisible = wallet.isHardware

        if(wallet.isHardware) {
            binding.hardware.setImageResource(R.drawable.ic_jade)
        }
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemWalletBinding {
        return ListItemWalletBinding.inflate(inflater, parent, false)
    }
}