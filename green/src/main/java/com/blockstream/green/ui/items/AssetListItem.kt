package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.blockstream.gdk.BalancePair
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAssetBinding
import com.blockstream.green.gdk.GreenSession
import com.blockstream.gdk.data.Asset
import com.blockstream.green.ui.looks.AssetListLook
import com.mikepenz.fastadapter.binding.AbstractBindingItem


data class AssetListItem constructor(
    val session: GreenSession,
    val balancePair: BalancePair,
    private val showInfo: Boolean,
    private val isLoading: Boolean
) : AbstractBindingItem<ListItemAssetBinding>() {
    override val type: Int
        get() = R.id.fastadapter_asset_item_id

    init {
        identifier = balancePair.first.hashCode().toLong() // asset.hashCode().toLong()
    }

    override fun bindView(binding: ListItemAssetBinding, payloads: List<Any>) {
        if(isLoading){
            binding.isLoading = true
            return
        }

        val asset: Asset? = session.getAsset(balancePair.first)

        val look = AssetListLook(balancePair.first, balancePair.second, asset, session)
        val res = binding.root.resources

        binding.isLoading = isLoading
        binding.name.text = look.name
        binding.ticker.text = look.ticker

        binding.primaryValue.text = look.balance

        val fiatValue = look.fiatValue

        binding.secondaryValue.text = fiatValue?.let {
            "%s %s".format(it.fiat, it.fiatCurrency)
        }
        binding.secondaryValue.isVisible = fiatValue != null

        binding.icon.setImageDrawable(look.icon(binding.root.context))

        binding.secondPart = showInfo

        if(showInfo) {
            binding.domain.text = res.getString(R.string.id_issuer_domain)
                .format(look.issuer ?: res.getString(R.string.id_unknown))

            binding.content.text = fiatValue?.let {
                "1 %s ~ %s %s".format(look.ticker, it.fiatRate, it.fiatCurrency)
            }
        }

        // disable the animation on some assets
        binding.root.isClickable = true
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemAssetBinding {
        return ListItemAssetBinding.inflate(inflater, parent, false)
    }
}
