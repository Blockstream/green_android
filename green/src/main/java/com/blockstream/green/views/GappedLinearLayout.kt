package com.blockstream.green.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.blockstream.green.R


class GappedLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var gap: Int = 0

    init {
        val attributes =
            context.obtainStyledAttributes(attrs, R.styleable.GappedLinearLayout)

        gap = attributes.getDimension(R.styleable.GappedLinearLayout_gap, 0f).toInt()

        attributes.recycle()
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (params is LinearLayout.LayoutParams) {
            if(index > 0 || (index == -1 && childCount > 0)){
                if (orientation == VERTICAL) {
                    params.topMargin += gap
                }else{
                    params.leftMargin += gap
                }
            }
        }

        super.addView(child, index, params)
    }
}