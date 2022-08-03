package com.blockstream.green.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.blockstream.green.R
import com.blockstream.green.databinding.GreenAlertViewBinding
import com.google.android.material.card.MaterialCardView

class GreenAlertView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.materialCardViewStyle
) : MaterialCardView(context, attrs, defStyleAttr) {

    val binding: GreenAlertViewBinding by lazy { GreenAlertViewBinding.inflate(LayoutInflater.from(context), this, true) }

    init {
        backgroundTintList = ContextCompat.getColorStateList(context, R.color.brand_surface)
    }

    var title: String?
        get() = binding.title
        set(value) {
            binding.title = value
        }

    var message: String?
        get() = binding.message
        set(value) {
            binding.message = value
        }

    fun setIconVisibility(isVisible: Boolean){
        binding.icon.isVisible = isVisible
    }

    fun setMaxLines(maxLines: Int){
        binding.messageTextView.maxLines = if(maxLines > 0) maxLines else Int.MAX_VALUE
    }

    fun primaryButton(text: String?, listener: OnClickListener?) {
        binding.buttonPrimary.text = text
        binding.buttonPrimary.setOnClickListener(listener)
        binding.buttonPrimary.isVisible = !text.isNullOrBlank()
        // add bottom padding if the button is hidden
        binding.container.updatePadding(bottom = binding.root.resources.getDimension(if(text.isNullOrBlank()) R.dimen.dp16 else R.dimen.dp0).toInt())
        binding.buttonPrimary.isClickable = listener != null
    }

    fun closeButton(listener: OnClickListener?) {
        binding.closeButton.setOnClickListener(listener)
        binding.closeButton.isVisible = listener != null
    }
}