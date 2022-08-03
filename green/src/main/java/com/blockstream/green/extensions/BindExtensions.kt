package com.blockstream.green.extensions

import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import com.blockstream.gdk.data.Account
import com.blockstream.gdk.data.AccountAsset
import com.blockstream.gdk.data.Utxo
import com.blockstream.green.R
import com.blockstream.green.databinding.AccountAssetLayoutBinding
import com.blockstream.green.databinding.AccountCardLayoutBinding
import com.blockstream.green.databinding.AssetLayoutBinding
import com.blockstream.green.databinding.UtxoLayoutBinding
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.gdk.balance
import com.blockstream.green.gdk.getAssetIcon
import com.blockstream.green.gdk.getAssetName
import com.blockstream.green.gdk.hasHistory
import com.blockstream.green.gdk.isPolicyAsset
import com.blockstream.green.gdk.needs2faActivation
import com.blockstream.green.gdk.policyAssetOrNull
import com.blockstream.green.looks.AssetLook
import com.blockstream.green.utils.toAmountLook
import com.blockstream.green.utils.toPixels
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.RoundedCornerTreatment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun AccountAssetLayoutBinding.bind(
    scope: CoroutineScope,
    accountAsset: AccountAsset,
    session: GdkSession,
    showBalance: Boolean = true,
    showEditIcon: Boolean = false,
) {
    val look = AssetLook(
        accountAsset.assetId,
        accountAsset.balance(session),
        session
    )

    this.showBalance = showBalance
    this.showEditIcon = showEditIcon
    account = accountAsset.account
    name = look.name

    if(showBalance){
        primaryValue = session.starsOrNull ?: ""
        secondaryValue = session.starsOrNull ?: ""
        scope.launch {
            primaryValue = session.starsOrNull ?: withContext(context = Dispatchers.IO) { look.balance(withUnit = true, withMinimumDigits = false) }
            secondaryValue = session.starsOrNull ?: withContext(context = Dispatchers.IO) {
                look.fiatValue.toAmountLook(
                    session = session,
                    isFiat = true,
                    withUnit = true
                )
            }
        }
    }

    icon.setImageDrawable(accountAsset.assetId.getAssetIcon(root.context, session))
}

fun AssetLayoutBinding.bind(
    scope: CoroutineScope,
    assetId: String,
    session: GdkSession,
    primaryValue: (suspend () -> String?)? = null,
    secondaryValue: (suspend () -> String?)? = null,
    showBalance: Boolean = false,
    showEditIcon: Boolean = false,
) {
    this.name = assetId.getAssetName(session)
    this.showBalance = showBalance
    this.showEditIcon = showEditIcon

    this.primaryValue = ""
    this.secondaryValue = ""
    if(showBalance){
        scope.launch {
            this@bind.primaryValue = withContext(context = Dispatchers.IO){ primaryValue?.invoke() } ?: ""
            this@bind.secondaryValue = withContext(context = Dispatchers.IO){ secondaryValue?.invoke() } ?: ""
        }
    }

    this.icon.setImageDrawable(assetId.getAssetIcon(root.context, session))
}

fun UtxoLayoutBinding.bind(
    scope: CoroutineScope,
    utxo: Utxo,
    session: GdkSession,
    showHash: Boolean,
    showName: Boolean
) {
    hash = if(showHash) "${utxo.txHash}:${utxo.index}" else null
    name = if(showName) utxo.assetId.getAssetName(session) else null

    amount = ""
    scope.launch {
        amount = withContext(context = Dispatchers.IO) {
            utxo.satoshi.toAmountLook(
                session,
                assetId = utxo.assetId,
                withUnit = true,
                withGrouping = true,
                withMinimumDigits = false
            )
        }
    }

    icon.setImageDrawable(utxo.assetId.getAssetIcon(context = root.context, session = session))
}

fun AccountCardLayoutBinding.bind(
    scope: CoroutineScope,
    account: Account,
    session: GdkSession,
    showArrow: Boolean = false,
    showCopy: Boolean = false,
    show2faActivation: Boolean = false
) {
    val context = root.context

    this.account = account
    this.showArrow = showArrow
    this.showCopy = showCopy

    val accountAssets = session.accountAssets(account)

    // If balance is not initiated yet, avoid showing a zero balance
    val policyAsset = accountAssets.policyAssetOrNull()

    // Don't update the values with empty string to avoid UI glitches
    // primaryValue = "" && secondaryValue = ""
    scope.launch {
        primaryValue = session.starsOrNull ?: withContext(context = Dispatchers.IO) {
            policyAsset.toAmountLook(
                session,
                assetId = account.network.policyAsset,
                withUnit = true,
                withGrouping = true,
                withMinimumDigits = false
            )
        }

        secondaryValue = session.starsOrNull ?: withContext(context = Dispatchers.IO) {
            policyAsset.toAmountLook(
                session,
                assetId = account.network.policyAsset,
                isFiat = true,
                withUnit = true,
                withGrouping = true,
                withMinimumDigits = false
            )
        }
    }


    needs2faActivation = show2faActivation && account.needs2faActivation(session)

    // Clear all icons
    assetsIcons.removeAllViews()

    var assetWithoutIconShown = false

    if(account.hasHistory(session)) {

        accountAssets.onEachIndexed { index, asset ->
            val isAssetWithoutIcon = if (asset.key.isPolicyAsset(session)) {
                false
            } else {
                session.getAssetDrawableOrNull(asset.key) == null
            }

            if (isAssetWithoutIcon) {
                if (assetWithoutIconShown) {
                    return@onEachIndexed
                } else {
                    assetWithoutIconShown = true
                }
            }

            ShapeableImageView(context).also { imageView ->
                val size = context.toPixels(30)
                val stroke = context.toPixels(1)

                val margin: Int = ((size / (1.3 + (0.1 * index))).toInt() * index)

                imageView.setImageDrawable(asset.key.getAssetIcon(context, session = session))
                imageView.layoutParams = FrameLayout.LayoutParams(
                    size,
                    size
                ).also {
                    it.updateMargins(left = margin)
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
                imageView.elevation = (accountAssets.size - index).toFloat()

                assetsIcons.addView(imageView)
            }
        }
    }
}