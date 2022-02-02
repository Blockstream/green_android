package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import com.blockstream.gdk.data.SubAccount
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAccountSettingsBinding
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.gdk.getAssetIcon
import com.blockstream.green.utils.toPixels
import com.blockstream.green.utils.updateAssetPadding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

data class AccountSettingsListItem constructor(
    val session: GreenSession,
    val subAccount: SubAccount
) : AbstractBindingItem<ListItemAccountSettingsBinding>() {
    override val type: Int
        get() = R.id.fastadapter_subaccount_settings_item_id

    init {
        identifier = subAccount.pointer
    }

    override fun bindView(binding: ListItemAccountSettingsBinding, payloads: List<Any>) {
        val context = binding.root.context

        binding.subAccount = subAccount
        binding.archivedChip.text = binding.root.context.getString(R.string.id_archived).lowercase()

        // Clear all icons
        binding.assetsIcons.removeAllViews()

        var assetWithoutIconShown = false
        session.walletBalances.get(subAccount.pointer.toInt())?.let { balances ->
            balances.onEachIndexed { index, balance ->
                if(balance.value > 0L) {
                    val isAssetWithoutIcon = if (balance.key == session.network.policyAsset) {
                        false
                    } else {
                        session.getAssetDrawableOrNull(balance.key) == null
                    }

                    if (isAssetWithoutIcon) {
                        if (assetWithoutIconShown) {
                            return@onEachIndexed
                        } else {
                            assetWithoutIconShown = true
                        }
                    }

                    ImageView(context).also { imageView ->
                        imageView.setImageDrawable(balance.key.getAssetIcon(context, session))
                        imageView.layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        ).also {
                            it.updateMargins(left = context.toPixels(12 * index))
                        }
                        imageView.adjustViewBounds = true
                        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                        imageView.elevation = (balances.size - index).toFloat()
                        imageView.updateAssetPadding(session, balance.key, 2)

                        binding.assetsIcons.addView(imageView)
                    }
                }
            }

            binding.assetsIcons.isVisible  = binding.assetsIcons.childCount > 0
        }

    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ) = ListItemAccountSettingsBinding.inflate(inflater, parent, false)
}