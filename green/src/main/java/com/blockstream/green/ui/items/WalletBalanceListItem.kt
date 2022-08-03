package com.blockstream.green.ui.items

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.underline
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import com.blockstream.gdk.BTC_POLICY_ASSET
import com.blockstream.green.R
import com.blockstream.green.data.Countly
import com.blockstream.green.databinding.ListItemWalletBalanceBinding
import com.blockstream.green.extensions.setOnClickListener
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.gdk.getAssetIcon
import com.blockstream.green.gdk.isPolicyAsset
import com.blockstream.green.utils.toAmountLook
import com.blockstream.green.utils.toAmountLookOrNa
import com.blockstream.green.utils.toPixels
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.RoundedCornerTreatment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KLogging

data class WalletBalanceListItem constructor(val session: GdkSession, val countly: Countly) :
    AbstractBindingItem<ListItemWalletBalanceBinding>() {
    override val type: Int
        get() = R.id.fastadapter_wallet_balance_item_id

    var isFiat = false

    private var view = 0

    private fun balanceInBtc() = session.starsOrNull ?: session.walletTotalBalanceFlow.value.toAmountLook(
        session = session,
        assetId = session.walletAssets.keys.firstOrNull(), // Expect the first asset to be the policy BTC or L-BTC
        isFiat = false,
        withUnit = true,
        withGrouping = true,
        overrideDenomination = view == 2
    )

    private fun balanceInFiat() = session.starsOrNull ?: session.walletTotalBalanceFlow.value.toAmountLookOrNa(
        session = session,
        assetId = session.walletAssets.keys.firstOrNull(), // Expect the first asset to be the policy BTC or L-BTC
        isFiat = true,
        withUnit = true,
        withGrouping = true
    )

    init {
        identifier = "WalletBalanceListItem".hashCode().toLong()
    }

    override fun createScope(): CoroutineScope = session.createScope(Dispatchers.Main)

    override fun bindView(binding: ListItemWalletBalanceBinding, payloads: List<Any>) {
        val context = binding.root.context
        val balance = session.walletTotalBalanceFlow.value

        binding.progressBar.isVisible = balance == -1L
        binding.balanceTextView.isInvisible = balance == -1L
        binding.hideAmounts = session.hideAmounts

        scope.launch {
            if (balance != -1L) {
                binding.balanceTextView.text = withContext(context = Dispatchers.IO) { balanceInBtc() }
                binding.fiatTextView.text = withContext(context = Dispatchers.IO) { balanceInFiat() }
            }
        }

        listOf(binding.balanceTextView,  binding.fiatTextView).setOnClickListener {
            view++

            if(view == 2 && session.getSettings(session.defaultNetwork)?.unit.equals(BTC_POLICY_ASSET, ignoreCase = true) || view > 2){
                view = 0
            }

            scope.launch {
                if (view == 1) {
                    binding.balanceTextView.text = withContext(context = Dispatchers.IO) { balanceInFiat() }
                    binding.fiatTextView.text = withContext(context = Dispatchers.IO) { balanceInBtc() }
                } else {
                    binding.balanceTextView.text = withContext(context = Dispatchers.IO) { balanceInBtc() }
                    binding.fiatTextView.text = withContext(context = Dispatchers.IO) { balanceInFiat() }
                }
            }

            countly.balanceConvert(session)
        }


        // Clear all icons
        binding.assetsIcons.removeAllViews()

        var assetWithoutIconShown = false

        val walletAssets = session.walletAssets.filterValues { it != -1L }.keys // Filter loading asset

        binding.assets = walletAssets.size

        binding.assetsTextView.text = buildSpannedString {
            underline {
                append(context.getString(R.string.id_d_assets_in_total, walletAssets.size))
            }
        }

        binding.assetsVisibility = when{
            walletAssets.size > 1 && session.walletHasHistory -> View.VISIBLE
            (session.activeBitcoin != null && session.activeLiquid != null && walletAssets.isEmpty()) -> View.INVISIBLE
            else -> {
                View.GONE
            }
        }

        var iconIndex = 0
        walletAssets.forEach { assetId ->
            val isAssetWithoutIcon = if (assetId.isPolicyAsset(session)) {
                false
            } else {
                session.getAssetDrawableOrNull(assetId) == null
            }

            if (isAssetWithoutIcon) {
                if (assetWithoutIconShown) {
                    return@forEach
                } else {
                    assetWithoutIconShown = true
                }
            }

            ShapeableImageView(context).also { imageView ->
                val size = context.toPixels(24)
                val stroke = context.toPixels(1)

                // val margin = (size / 2) * iconIndex
                val margin: Int = ((size / (2 + (0.1 * iconIndex))).toInt() * iconIndex)

                imageView.setImageDrawable(assetId.getAssetIcon(context, session = session))
                imageView.layoutParams = FrameLayout.LayoutParams(
                    size,
                    size
                ).also {
                    it.gravity = Gravity.END
                    it.updateMargins(right = margin)
                }

                imageView.setBackgroundColor(ContextCompat.getColor(context, R.color.black))
                imageView.strokeColor = ContextCompat.getColorStateList(context, R.color.black)
                imageView.strokeWidth = stroke.toFloat()
                imageView.updatePadding(
                    stroke,
                    stroke,
                    stroke,
                    stroke
                ) // update padding to match the stroke
                imageView.shapeAppearanceModel = imageView.shapeAppearanceModel.toBuilder()
                    .setAllCorners(RoundedCornerTreatment())
                    .setAllCornerSizes(RelativeCornerSize(0.5f))
                    .build()
                imageView.adjustViewBounds = true
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                imageView.elevation = (walletAssets.size - iconIndex).toFloat()

                binding.assetsIcons.addView(imageView)

                iconIndex++
            }
        }
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemWalletBalanceBinding {
        return ListItemWalletBalanceBinding.inflate(inflater, parent, false)
    }

    companion object: KLogging()
}