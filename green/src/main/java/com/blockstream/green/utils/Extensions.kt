package com.blockstream.green.utils

import android.view.ViewParent
import android.widget.ImageView
import androidx.core.widget.doAfterTextChanged
import com.blockstream.green.R
import com.blockstream.green.gdk.GreenSession
import com.google.android.material.textfield.TextInputEditText
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

fun ImageView.updateAssetPadding(session: GreenSession, assetId: String, padding : Int){
    var imagePadding = 0
    if(session.isLiquid){
        if(session.policyAsset != assetId && session.hasAssetIcon(assetId)){
            imagePadding = context.toPixels(padding)
        }
    }
    setPadding(imagePadding, imagePadding, imagePadding, imagePadding)
}

fun TextInputEditText.findTextInputLayoutParent(): TextInputLayout? {
    return parent?.findTextInputLayoutParent(3)
}

fun ViewParent.findTextInputLayoutParent(maxDepth: Int): TextInputLayout? {
    if (parent is TextInputLayout) {
        return parent as TextInputLayout
    } else if (maxDepth > 0) {
        return parent.findTextInputLayoutParent(maxDepth - 1)
    }
    return null
}