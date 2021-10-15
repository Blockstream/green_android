package com.blockstream.green.views

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import com.blockstream.green.R
import com.google.android.material.textfield.TextInputLayout

// Based on https://stackoverflow.com/a/61948923/914358
class RightTextInputLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TextInputLayout(context, attrs, defStyleAttr) {

    override fun setErrorEnabled(enabled: Boolean) {
        super.setErrorEnabled(enabled)
        if (enabled) {
            with(findViewById<TextView>(R.id.textinput_error)) {
                // this will work as long as errorView's layout width is
                // MATCH_PARENT -- it is, at least now in material:1.4.0
                textAlignment = TextView.TEXT_ALIGNMENT_VIEW_END
            }
        }
    }

    override fun setHelperTextEnabled(enabled: Boolean) {
        super.setHelperTextEnabled(enabled)
        if (enabled) {
            with(findViewById<TextView>(R.id.textinput_helper_text)) {
                // this will work as long as errorView's layout width is
                // MATCH_PARENT -- it is, at least now in material:1.4.0
                textAlignment = TextView.TEXT_ALIGNMENT_VIEW_END
            }
        }
    }
}