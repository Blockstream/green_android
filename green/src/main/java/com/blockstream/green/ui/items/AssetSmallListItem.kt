package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAssetSmallBinding
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.gdk.getAssetIcon
import com.blockstream.green.gdk.getAssetName
import com.blockstream.green.gdk.getAssetTicker
import com.mikepenz.fastadapter.binding.AbstractBindingItem


data class AssetSmallListItem constructor(
    val session: GdkSession,
    val assetId: String
) : AbstractBindingItem<ListItemAssetSmallBinding>() {

    override val type: Int
        get() = R.id.fastadapter_asset_small_item_id

    init {
        identifier = (assetId.ifBlank { "AssetSmallListItem" }).hashCode().toLong()
    }

    val name
        get() = assetId.getAssetName(session)

    val ticker
        get() = assetId.getAssetTicker(session)

    override fun bindView(binding: ListItemAssetSmallBinding, payloads: List<Any>) {
        binding.name.text = name
        binding.icon.setImageDrawable(assetId.getAssetIcon(binding.root.context, session))
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemAssetSmallBinding {
        return ListItemAssetSmallBinding.inflate(inflater, parent, false)
    }
}
