package com.blockstream.green.utils


import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.text.TextUtils
import android.util.SparseArray
import android.view.View
import android.view.ViewParent
import android.widget.ImageView
import android.widget.TextView
import androidx.core.util.valueIterator
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.MutableLiveData
import com.blockstream.green.R
import com.blockstream.green.gdk.GreenSession
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

fun Activity.snackbar(text: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(findViewById(android.R.id.content), text, duration).show()
}

fun TextInputLayout.endIconCopyMode(pasteListener: (() -> Unit)? = null ) {
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
            pasteListener?.invoke()
        } else {
            editText?.text?.clear()
        }
    }
}

fun TextView.setDrawable(drawableLeft: Drawable? = null, drawableRight: Drawable? = null, padding: Int = 8) {
    compoundDrawablePadding = padding.dp(this)
    setCompoundDrawablesWithIntrinsicBounds(drawableLeft, null, drawableRight, null)
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

fun <E: View> Collection<E>.setOnClickListener(onClickListener: (e: View) -> Unit) {
    this.forEach {
        it.setOnClickListener(onClickListener)
    }
}

fun CharSequence?.isEmailValid(): Boolean {
    return !TextUtils.isEmpty(this) && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun MutableLiveData<String>.string() : String = value ?: ""
fun MutableLiveData<Boolean>.boolean() : Boolean = value ?: false

// Helper fn for Data Binding as the original fn is InlineOnly
fun String?.isBlank() = isNullOrBlank()
fun String?.isNotBlank() = !isNullOrBlank()

fun String.fromHtml(): Spanned {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(this, Html.FROM_HTML_MODE_COMPACT)
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(this)
    }
}

inline fun <reified V> SparseArray<V>.toList() = valueIterator().asSequence().toList()