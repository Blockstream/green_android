package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.gdk.data.Utxo
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemUtxoBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem


data class UtxoListItem constructor(val utxo : Utxo) : AbstractBindingItem<ListItemUtxoBinding>() {
    override val type: Int
        get() = R.id.fastadapter_utxo_item_id

    init {
        identifier = utxo.hashCode().toLong()
    }

    override fun bindView(binding: ListItemUtxoBinding, payloads: List<Any>) {

        binding.materiaCard.isChecked = false

        binding.address = utxo.txHash
        binding.ticker = "BTC"
        binding.amount = utxo.satoshi.toString()

        binding.root.setOnClickListener {
            binding.materiaCard.isChecked = !binding.materiaCard.isChecked
        }

//        val asset: Asset? = session.getAsset(balancePair.first)
//
//        val look = AssetListLook(balancePair.first, balancePair.second, asset, session)
//        val res = binding.root.resources
//
//        binding.isLoading = isLoading
//        binding.name.text = look.name
//        binding.ticker.text = look.ticker
//
//        binding.primaryValue.text = look.balance
//
//        val fiatValue = look.fiatValue
//
//        binding.secondaryValue.text = fiatValue.fiat(session = session, withUnit = true)
//        binding.secondaryValue.isVisible = fiatValue != null
//
//        binding.icon.setImageDrawable(look.icon(binding.root.context))
//
//        binding.secondPart = showInfo
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemUtxoBinding {
        return ListItemUtxoBinding.inflate(inflater, parent, false)
    }
}
