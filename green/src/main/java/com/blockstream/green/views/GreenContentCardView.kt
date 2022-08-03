package com.blockstream.green.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.core.view.isVisible
import com.blockstream.green.R
import com.blockstream.green.databinding.GreenContentCardViewBinding
import com.google.android.material.card.MaterialCardView

class GreenContentCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.materialCardViewStyle
) : MaterialCardView(context, attrs, defStyleAttr) {

    internal var icon: ImageView

    private val binding: GreenContentCardViewBinding by lazy {
        GreenContentCardViewBinding.inflate(LayoutInflater.from(context), this, true) }

    init {
        icon = binding.icon

        val attributes =
            context.obtainStyledAttributes(attrs, R.styleable.GreenContentCardView)

        attributes.getString(R.styleable.GreenContentCardView_titleText)?.let {
            binding.title = it
        }

        attributes.getString(R.styleable.GreenContentCardView_captionText)?.let {
            binding.caption = it
        }

        attributes.getDrawable(R.styleable.GreenContentCardView_icon)?.let {
            binding.icon.setImageDrawable(it)
            binding.icon.isVisible = true
        }

        if (isCheckable) {
            setOnClickListener {
                toggle()
            }
        }

        attributes.recycle()
    }

    fun setTitle(title: String?){
        binding.title = title
    }

    fun setCaption(caption: String?){
        binding.caption = caption
    }

    fun setIcon(resId: Int) {
        binding.icon.setImageResource(resId)
        binding.icon.isVisible = resId > 0
    }
}