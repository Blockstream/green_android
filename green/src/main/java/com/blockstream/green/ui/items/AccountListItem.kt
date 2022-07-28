package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import com.blockstream.gdk.data.SubAccount
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAccountBinding
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.gdk.getAssetIcon
import com.blockstream.green.utils.getBitcoinOrLiquidUnit
import com.blockstream.green.utils.toAmountLook
import com.blockstream.green.utils.toPixels
import com.blockstream.green.utils.updateAssetPadding
import com.mikepenz.fastadapter.binding.AbstractBindingItem


data class AccountListItem constructor(
    val session: GreenSession,
    val subAccount: SubAccount,
    val isTopAccount: Boolean = false,
    var showFakeCard: Boolean = false,
    var isAccountListOpen: Boolean = false
) : AbstractBindingItem<ListItemAccountBinding>() {
    override val type: Int
        get() = R.id.fastadapter_account_item_id

    init {
        identifier = "AccountListItem".hashCode() + subAccount.pointer
    }

    override fun bindView(binding: ListItemAccountBinding, payloads: List<Any>) {
        val context = binding.root.context

        binding.isTopAccount = isTopAccount
        binding.isAccountListOpen = isAccountListOpen
        binding.isMainnet = session.isMainnet
        binding.isLiquid = session.isLiquid
        binding.isSinglesig = session.isSinglesig
        binding.isWatchOnly = session.isWatchOnly
        binding.subAccount = subAccount

        if(isTopAccount){
            binding.fakeAccountCard.isInvisible = !showFakeCard || isAccountListOpen
        }else{
            binding.fakeAccountCard.isVisible = false
        }

        val policyAsset = session.walletBalances.get(subAccount.pointer.toInt())?.entries?.firstOrNull()

        binding.balance = policyAsset?.value?.toAmountLook(session, withUnit = false, withGrouping = true, withMinimumDigits = false)
        binding.ticker = getBitcoinOrLiquidUnit(session)

        if(session.isLiquid){
            // Clear all icons
            binding.assetsIcons.removeAllViews()

            var assetWithoutIconShown = false
            session.walletBalances.get(subAccount.pointer.toInt())?.let { balances ->
                balances.onEachIndexed { index, balance ->

                    val isAssetWithoutIcon = if (balance.key == session.network.policyAsset) {
                        false
                    } else {
                        session.getAssetDrawableOrNull(balance.key) == null
                    }

                    if(isAssetWithoutIcon){
                        if(assetWithoutIconShown){
                            return@onEachIndexed
                        }else{
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
        }
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemAccountBinding {
        return ListItemAccountBinding.inflate(inflater, parent, false)
    }
}