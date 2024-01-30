package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.blockstream.common.data.Denomination
import com.blockstream.common.extensions.networkForAsset
import com.blockstream.common.gdk.EnrichedAssetPair
import com.blockstream.common.gdk.GdkSession
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAssetBinding
import com.blockstream.green.extensions.bind
import com.blockstream.green.extensions.context
import com.blockstream.green.looks.AssetLook
import com.blockstream.green.utils.toAmountLook
import com.blockstream.green.utils.toPixels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


data class AssetListItem constructor(
    val session: GdkSession,
    val assetPair: EnrichedAssetPair,
    val showBalance: Boolean = false,
    val isLoading: Boolean = false,
    val withBottomMargin : Boolean = true
) : AbstractExpandableBindingItem<ListItemAssetBinding>() {
    override val type: Int
        get() = R.id.fastadapter_asset_item_id

    val network by lazy {
        assetPair.first.assetId.networkForAsset(session)
    }
    
    private val look = AssetLook(assetPair.first.assetId, assetPair.second, session)

    val name
        get() = look.name

    val enrichedAsset
        get() = assetPair.first

    init {
        identifier = assetPair.first.hashCode().toLong()
    }

    override fun createScope(): CoroutineScope = session.createScope(dispatcher = Dispatchers.Main)

    override fun bindView(binding: ListItemAssetBinding, payloads: List<Any>) {
        if(isLoading){
            binding.isLoading = true
            return
        }

        binding.card.strokeColor = ContextCompat.getColor(binding.context(), if(isExpanded) R.color.brand_green else android.R.color.transparent)
        binding.card.strokeWidth = if(isExpanded) binding.context().toPixels(2) else 0

        binding.isLoading = false
        binding.withBottomMargin = withBottomMargin

        binding.asset.bind(
            scope = scope,
            assetId = assetPair.first.assetId,
            session = session,
            showBalance = showBalance,
            primaryValue = {
                if (showBalance) session.starsOrNull ?: look.balance(
                    withUnit = true,
                    withMinimumDigits = false
                ) else null
            },
            secondaryValue = {
                if (showBalance) session.starsOrNull ?: look.fiatValue.toAmountLook(
                    session = session,
                    denomination = Denomination.fiat(session),
                    withUnit = true
                ) ?: "-" else null
            }
        )

        if (enrichedAsset.isAnyAsset && !enrichedAsset.isAmp) {
            // Change asset name
            binding.asset.name = context(binding).getString(R.string.id_receive_any_liquid_asset)
            binding.asset.icon.setImageDrawable(
                ContextCompat.getDrawable(
                    context(binding),
                    R.drawable.ic_liquid_asset
                )
            )
        } else if (enrichedAsset.isAnyAsset && enrichedAsset.isAmp) {
            // Change asset name
            binding.asset.name = context(binding).getString(R.string.id_receive_any_amp_asset)
            binding.asset.icon.setImageDrawable(
                ContextCompat.getDrawable(
                    context(binding),
                    R.drawable.ic_amp_asset
                )
            )
        }


        // disable the animation on some assets
        binding.root.isClickable = true
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemAssetBinding {
        return ListItemAssetBinding.inflate(inflater, parent, false)
    }
}
