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
import com.blockstream.common.data.Denomination
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.gdk.GdkSession
import com.blockstream.green.R
import com.blockstream.green.data.CountlyAndroid
import com.blockstream.green.databinding.ListItemWalletBalanceBinding
import com.blockstream.green.gdk.getAssetDrawableOrNull
import com.blockstream.green.gdk.getAssetIcon
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

data class WalletBalanceListItem constructor(val session: GdkSession, val countly: CountlyAndroid) :
    AbstractBindingItem<ListItemWalletBalanceBinding>() {
    override val type: Int
        get() = R.id.fastadapter_wallet_balance_item_id

    var denomination: Denomination = Denomination.default(session)

    private suspend fun balanceInBtc() = session.starsOrNull ?: session.walletTotalBalance.value.toAmountLook(
        session = session,
        assetId = session.walletAssets.value.policyId,
        withUnit = true,
        withGrouping = true
    )

    private suspend fun balanceInFiat() = session.starsOrNull ?: session.walletTotalBalance.value.toAmountLookOrNa(
        session = session,
        assetId = session.walletAssets.value.policyId,
        denomination = Denomination.fiat(session),
        withUnit = true,
        withGrouping = true
    )

    init {
        identifier = "WalletBalanceListItem".hashCode().toLong()
    }

    override fun createScope(): CoroutineScope = session.createScope(Dispatchers.Main)

    fun reset(denomination: Denomination) {
        this.denomination = denomination
    }

    suspend fun updateBalanceView(binding: ListItemWalletBalanceBinding) {
        val balance = session.walletTotalBalance.value

        if (balance != -1L) {
            if (denomination.isFiat) {
                binding.balanceTextView.text = withContext(context = Dispatchers.IO) { balanceInFiat() }
                binding.fiatTextView.text = withContext(context = Dispatchers.IO) { balanceInBtc() }
            } else {
                binding.balanceTextView.text = withContext(context = Dispatchers.IO) { balanceInBtc() }
                binding.fiatTextView.text = withContext(context = Dispatchers.IO) { balanceInFiat() }
            }
        }
    }

    override fun bindView(binding: ListItemWalletBalanceBinding, payloads: List<Any>) {
        val context = binding.root.context
        val balance = session.walletTotalBalance.value

        binding.progressBar.isVisible = balance == -1L
        binding.balanceTextView.isInvisible = balance == -1L
        binding.buttonDenomination.isInvisible = balance == -1L
        binding.hideAmounts = session.hideAmounts

        scope.launch {
            updateBalanceView(binding)
        }

        // Clear all icons
        binding.assetsIcons.removeAllViews()

        var assetWithoutIconShown = false

        // Filter loading asset & zero balance assets
        val walletAssets = session.walletAssets.value.withFunds.keys

        binding.assets = walletAssets.size

        binding.assetsTextView.text = buildSpannedString {
            underline {
                append(context.getString(R.string.id_d_assets_in_total, walletAssets.size))
            }
        }

        binding.assetsVisibility = when {
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
                session.getAssetDrawableOrNull(context, assetId) == null
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