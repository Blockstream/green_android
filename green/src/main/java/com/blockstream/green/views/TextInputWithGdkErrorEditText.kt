package com.blockstream.green.views

import android.content.Context
import android.util.AttributeSet
import com.blockstream.green.adapters.setGdkError
import com.blockstream.green.utils.findTextInputLayoutParent
import com.google.android.material.R
import com.google.android.material.textfield.TextInputEditText

class TextInputWithGdkErrorEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.editTextStyle
) : TextInputEditText(context, attrs, defStyleAttr) {

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)

        // Restore error text if existed
        this.findTextInputLayoutParent()?.let { textInputLayout ->
            if (text.isNullOrBlank()) {
                textInputLayout.error = null
            } else {
                textInputLayout.getTag("errorText".hashCode())?.let { error ->
                    if (error is String) {
                        setGdkError(textInputLayout, error)
                    }
                }
            }
        }
    }
}