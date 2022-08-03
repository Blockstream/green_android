package com.blockstream.green.extensions


import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.text.TextUtils
import android.util.LongSparseArray
import android.util.SparseArray
import android.view.View
import android.view.ViewParent
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.util.forEach
import androidx.core.util.valueIterator
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.MutableLiveData
import androidx.viewbinding.ViewBinding
import com.blockstream.green.R
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.gdk.isPolicyAsset
import com.blockstream.green.utils.dp
import com.blockstream.green.utils.getClipboard
import com.blockstream.green.utils.toPixels
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

fun ViewBinding.context(): Context = root.context

fun Activity.snackbar(text: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(findViewById(android.R.id.content), text, duration).show()
}
fun TextInputLayout.endIconPadding(margin: Int = 24){
    try {
        val endLayout = suffixTextView.parent as View
        endLayout.updatePadding(right = context.toPixels(margin))
    }catch (e: Exception){
        e.printStackTrace()
    }
}

fun TextInputLayout.endIconCustomMode(intRes: Int = R.drawable.ic_clipboard, listener: (() -> Unit)? = null ) {
    endIconMode = TextInputLayout.END_ICON_CUSTOM
    // don't replace custom icon with the error exclamation mark
    errorIconDrawable = null

    setEndIconDrawable(if (editText?.text.isNullOrBlank()) intRes else com.google.android.material.R.drawable.mtrl_ic_cancel)

    editText?.doAfterTextChanged {
        setEndIconDrawable(if (it.isNullOrBlank()) intRes else com.google.android.material.R.drawable.mtrl_ic_cancel)
    }

    setEndIconOnClickListener {
        if (editText?.text.isNullOrBlank()) {
            editText?.setText(getClipboard(context))
            listener?.invoke()
        } else {
            editText?.text?.clear()
        }
    }
}

fun TextInputEditText.setStartDrawable(resource: Int, size: Int = 28) {
    setStartDrawable(drawable = ContextCompat.getDrawable(context, resource), size = size)
}

fun TextInputEditText.setStartDrawable(drawable: Drawable?, size: Int = 28) {
    @Suppress("NAME_SHADOWING") val size = context.toPixels(size)
    val resizedBitmap = drawable?.toBitmap(width = size, height = size)
    val resizedDrawable = resizedBitmap?.let { BitmapDrawable(resources, Bitmap.createScaledBitmap(it, size, size, true)) }
    setCompoundDrawablesRelativeWithIntrinsicBounds(resizedDrawable, null, null, null)
}

fun TextView.setDrawable(drawableLeft: Drawable? = null, drawableRight: Drawable? = null, padding: Int = 8) {
    compoundDrawablePadding = padding.dp(this)
    setCompoundDrawablesWithIntrinsicBounds(drawableLeft, null, drawableRight, null)
}

fun ShapeableImageView.updateAssetContentPadding(session: GdkSession, assetId: String, padding : Int){
    var imagePadding = 0
    if(!assetId.isPolicyAsset(session) && session.hasAssetIcon(assetId)){
        imagePadding = context.toPixels(padding)
    }
    // Issue https://github.com/material-components/material-components-android/issues/2063
    post {
        setContentPadding(imagePadding, imagePadding, imagePadding, imagePadding)
    }
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
    return !TextUtils.isEmpty(this) && android.util.Patterns.EMAIL_ADDRESS.matcher(this ?: "").matches()
}

fun MutableLiveData<String>.string() : String = value ?: ""
fun MutableLiveData<Boolean>.boolean() : Boolean = value ?: false
fun MutableLiveData<Boolean>.toggle() : Boolean = (value?.let { !it } ?: false).also { this.value = it }

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
inline fun <reified V> LongSparseArray<V>.toList() = valueIterator().asSequence().toList()

public inline fun <T, R> LongSparseArray<T>.map(transform: (T) -> R): LongSparseArray<R> {
    return LongSparseArray<R>(size().coerceAtLeast(1)).also {
        this.forEach { key, value ->
            it.put(key, transform(value))
        }
    }
}