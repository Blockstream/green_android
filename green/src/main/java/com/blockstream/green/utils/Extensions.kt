package com.blockstream.green.utils

import androidx.core.widget.doAfterTextChanged
import com.blockstream.green.R
import com.google.android.material.textfield.TextInputLayout

fun TextInputLayout.endIconCopyMode() {
    endIconMode = TextInputLayout.END_ICON_CUSTOM
    // don't replace custom icon with the error exclamation mark
    errorIconDrawable = null

    setEndIconDrawable(if (editText?.text.isNullOrBlank()) R.drawable.ic_baseline_content_paste_24 else com.google.android.material.R.drawable.mtrl_ic_cancel)

    editText?.doAfterTextChanged {
        setEndIconDrawable(if (it.isNullOrBlank()) R.drawable.ic_baseline_content_paste_24 else com.google.android.material.R.drawable.mtrl_ic_cancel)
    }

    setEndIconOnClickListener {
        if (editText?.text.isNullOrBlank()) {
            editText?.setText(getClipboard(context))
        } else {
            editText?.text?.clear()
        }
    }
}

