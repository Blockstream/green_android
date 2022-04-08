package com.blockstream.green.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isGone
import com.blockstream.green.R
import com.blockstream.green.databinding.GreenToolbarBinding
import com.google.android.material.appbar.MaterialToolbar

class GreenToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.toolbarStyle
) : MaterialToolbar(context, attrs, defStyleAttr) {

    private var binding: GreenToolbarBinding =
        GreenToolbarBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        // Re apply the title after our custom binding was initiated
        if (!TextUtils.isEmpty(title)) {
            title = title
        }
    }

    override fun setTitle(title: CharSequence?) {
        binding.title.text = title
    }

    override fun getTitle(): CharSequence = binding.title.text

    override fun setSubtitle(subtitle: CharSequence?) {
        super.setSubtitle(subtitle)
        binding.subtitle.text = subtitle
        binding.subtitle.isGone = subtitle.isNullOrBlank()
    }

    override fun setLogo(logo: Drawable?) {
        binding.icon.isGone = logo == null
        binding.icon.setImageDrawable(logo)
    }

    fun setLogoClickListener(listener : OnClickListener) {
        binding.icon.setOnClickListener(listener)
    }

    fun setBubble(bubble: Drawable?) {
        binding.bubbleIcon.isGone = bubble == null
        binding.bubbleIcon.setImageDrawable(bubble)
    }

    fun setButton(button: CharSequence? = null, buttonListener: OnClickListener? = null) {
        binding.button.isGone = button == null
        binding.button.text = button
        binding.button.setOnClickListener(buttonListener)
    }
}