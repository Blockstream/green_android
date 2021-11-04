package com.blockstream.green.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Checkable
import android.widget.CompoundButton
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.blockstream.green.R
import com.blockstream.green.databinding.GreenSwitchBinding
import com.google.android.material.switchmaterial.SwitchMaterial


class GreenSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), Checkable {
    private var switchInvalid: Boolean = false
    private var binding: GreenSwitchBinding =
        GreenSwitchBinding.inflate(LayoutInflater.from(context), this, true)

    val switch: SwitchMaterial
        get() = binding.switchmaterial

    init {

        val attributes =
            context.obtainStyledAttributes(attrs, R.styleable.GreenSwitch)

        attributes.getString(R.styleable.GreenSwitch_titleText)?.let {
            binding.title.text = it
        }

        attributes.getString(R.styleable.GreenSwitch_captionText)?.let {
            binding.caption.text = it
            binding.caption.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
        }

        attributes.getDrawable(R.styleable.GreenSwitch_icon)?.let {
            binding.icon.setImageDrawable(it)
        }

        // Disabling GreenSwitch only works when initiated as a attribute
        // that is the only use-case at the time
        binding.switchmaterial.isEnabled = attributes.getBoolean(
            R.styleable.GreenSwitch_android_enabled,
            true
        ).also {
            if(!it){
                binding.title.setTextColor(ContextCompat.getColor(context, R.color.color_on_surface_emphasis_low))
                binding.caption.setTextColor(ContextCompat.getColor(context, R.color.color_on_surface_emphasis_low))
                binding.icon.setColorFilter(ContextCompat.getColor(context, R.color.color_on_surface_emphasis_low), PorterDuff.Mode.SRC_IN)
            }
        }

        binding.switchmaterial.isChecked = attributes.getBoolean(
            R.styleable.GreenSwitch_android_checked,
            false
        )

        setOnClickListener {
            if(binding.switchmaterial.isEnabled) {
                binding.switchmaterial.toggle()
            }
        }

        attributes.recycle()
    }

    fun setOnCheckedChangeListener(listener: CompoundButton.OnCheckedChangeListener?) {
        binding.switchmaterial.setOnCheckedChangeListener(listener)
    }

    override fun setChecked(checked: Boolean) {
        binding.switchmaterial.isChecked = checked
    }

    override fun isChecked(): Boolean {
        return binding.switchmaterial.isChecked
    }

    fun setCaption(text: String?){
        binding.caption.text = text
        binding.caption.visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    fun setInvalid(invalid: Boolean) {
        switchInvalid = invalid
        binding.switchmaterial.thumbTintList = ContextCompat.getColorStateList(
            context,
            if (invalid) R.color.switch_thumb_invalid else R.color.switch_thumb
        )
    }

    fun isInvalid(): Boolean {
        return switchInvalid
    }

    override fun toggle() {
        binding.switchmaterial.toggle()
    }
}