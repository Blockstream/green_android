package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.gdk.data.Asset
import com.blockstream.green.databinding.ListItemAssetSmallBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem


data class AssetSmallListItem constructor(
    val asset: Asset
) : AbstractBindingItem<ListItemAssetSmallBinding>() {
    override val type: Int
        get() = R.id.fastadapter_asset_small_item_id

    init {
        identifier = asset.hashCode().toLong() // asset.hashCode().toLong()
    }

    override fun bindView(binding: ListItemAssetSmallBinding, payloads: List<Any>) {

        binding.name.text = asset.name
        binding.icon.setImageResource(R.drawable.ic_bitcoin_network_60)

    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemAssetSmallBinding {
        return ListItemAssetSmallBinding.inflate(inflater, parent, false)
    }
}
