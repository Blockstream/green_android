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
        set(title)
    }

    override fun setSubtitle(subtitle: CharSequence?) {
        set(title = title, subtitle = subtitle)
    }

    override fun setLogo(drawable: Drawable?) {
        set(title, subtitle, drawable)
    }

    fun set(
        title: CharSequence? = null,
        subtitle: CharSequence? = null,
        drawableLogo: Drawable? = null,
        drawableBubbleIcon: Drawable? = null,
        button: CharSequence? = null,
        buttonListener: OnClickListener? = null
    ) {
        binding.title.text = title

        binding.subtitle.text = subtitle
        binding.subtitle.isGone = subtitle.isNullOrBlank()

        binding.icon.isGone = drawableLogo == null
        binding.icon.setImageDrawable(drawableLogo)

        binding.bubbleIcon.isGone = drawableBubbleIcon == null
        binding.bubbleIcon.setImageDrawable(drawableBubbleIcon)

        binding.button.isGone = button == null
        binding.button.text = button
        binding.button.setOnClickListener(buttonListener)
    }
}