package com.blockstream.green.views

import android.content.Context
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.blockstream.green.R
import com.blockstream.green.databinding.GreenContentCardViewBinding
import com.google.android.material.card.MaterialCardView

class GreenContentCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.materialCardViewStyle
) : MaterialCardView(context, attrs, defStyleAttr) {

    internal var title: TextView
    internal var caption: TextView
    internal var icon: ImageView

    private var binding: GreenContentCardViewBinding =
        GreenContentCardViewBinding.inflate(LayoutInflater.from(context), this, true)

    init {

        title = binding.title
        caption = binding.caption
        icon = binding.icon

        val attributes =
            context.obtainStyledAttributes(attrs, R.styleable.GreenContentCardView)

        attributes.getString(R.styleable.GreenContentCardView_titleText)?.let {
            binding.title.text = it
        }

        attributes.getString(R.styleable.GreenContentCardView_captionText)?.let {
            binding.caption.text = it
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

    // TODO Temp method to change the colors, should be removed or refactored
    fun disable() {
        binding.title.setTextColor(ContextCompat.getColor(context, R.color.color_on_surface_emphasis_disabled))
        binding.caption.setTextColor(ContextCompat.getColor(context, R.color.color_on_surface_emphasis_disabled))
        binding.icon.setColorFilter(ContextCompat.getColor(context, R.color.color_on_surface_emphasis_disabled), PorterDuff.Mode.SRC_IN)
    }
}