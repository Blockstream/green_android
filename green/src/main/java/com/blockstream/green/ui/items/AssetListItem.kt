package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.blockstream.gdk.BalancePair
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAssetBinding
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.ui.looks.AssetLook
import com.blockstream.green.utils.fiat
import com.blockstream.green.utils.updateAssetPadding
import com.mikepenz.fastadapter.binding.AbstractBindingItem


data class AssetListItem constructor(
    val session: GreenSession,
    val balancePair: BalancePair,
    private val showInfo: Boolean,
    private val isLoading: Boolean
) : AbstractBindingItem<ListItemAssetBinding>() {
    override val type: Int
        get() = R.id.fastadapter_asset_item_id
    
    private val look = AssetLook(balancePair.first, balancePair.second, session)

    val name
        get() = look.name

    init {
        identifier = (if(balancePair.first.isBlank()) "AssetListItem" else balancePair.first).hashCode().toLong()
    }

    override fun bindView(binding: ListItemAssetBinding, payloads: List<Any>) {
        if(isLoading){
            binding.isLoading = true
            return
        }

        val res = binding.root.resources

        binding.isLoading = isLoading
        binding.name.text = look.name
        binding.ticker.text = look.ticker

        binding.primaryValue.text = look.balance()

        val fiatValue = look.fiatValue

        binding.secondaryValue.text = fiatValue.fiat(session = session, withUnit = true)
        binding.secondaryValue.isVisible = fiatValue != null

        binding.icon.setImageDrawable(look.icon(binding.root.context))
        binding.icon.updateAssetPadding(session, balancePair.first, 5)

        binding.secondPart = showInfo

        if(showInfo) {
            binding.domain.text = res.getString(R.string.id_issuer_domain_s)
                .format(look.issuer ?: res.getString(R.string.id_unknown))

            binding.content.text = fiatValue?.let {
                "1 %s ~ %s %s".format(look.ticker, it.fiatRate ?: "n/a", it.fiatCurrency)
            }
        }

        // disable the animation on some assets
        binding.root.isClickable = true
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemAssetBinding {
        return ListItemAssetBinding.inflate(inflater, parent, false)
    }
}
