package com.blockstream.green.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import com.blockstream.green.R


class DividerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {


    init {
        setBackgroundResource(R.color.color_on_surface_divider)
    }

    override fun getSuggestedMinimumHeight(): Int {
        val dip = 1f
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, resources.displayMetrics).toInt()
    }

}