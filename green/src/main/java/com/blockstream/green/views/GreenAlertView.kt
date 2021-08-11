package com.blockstream.green.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.blockstream.green.R
import com.blockstream.green.databinding.GreenAlertViewBinding
import com.google.android.material.card.MaterialCardView

class GreenAlertView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.materialCardViewStyle
) : MaterialCardView(context, attrs, defStyleAttr) {

    private var binding: GreenAlertViewBinding =
        GreenAlertViewBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)
    }

    var title: String
        get() = binding.title.text.toString()
        set(value) {
            binding.title.text = value
        }

    var message: String
        get() = binding.message.text.toString()
        set(value) {
            binding.message.text = value
        }

    fun primaryButton(text: String, listener: OnClickListener) {
        binding.buttonPrimary.text = text
        binding.buttonPrimary.setOnClickListener(listener)
        binding.buttonPrimary.visibility = VISIBLE
    }

    fun closeButton(listener: OnClickListener) {
        binding.closeButton.setOnClickListener(listener)
        binding.closeButton.visibility = VISIBLE
    }

}