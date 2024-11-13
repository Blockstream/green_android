package com.blockstream.green.extensions


import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.devices.DeviceBrand
import com.blockstream.common.extensions.isPolicyAsset
import com.blockstream.common.gdk.GdkSession
import com.blockstream.green.R
import com.blockstream.green.utils.dp
import com.blockstream.green.utils.getClipboard
import com.blockstream.green.utils.toPixels
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

fun DeviceBrand.icon(): Int = when (this) {
    DeviceBrand.Blockstream -> R.drawable.ic_blockstream
    DeviceBrand.Ledger -> R.drawable.ic_ledger
    DeviceBrand.Trezor -> R.drawable.ic_trezor
}


fun DeviceBrand.deviceIcon(): Int = when (this) {
    DeviceBrand.Blockstream -> R.drawable.blockstream_jade_device
    DeviceBrand.Ledger -> R.drawable.ledger_device
    DeviceBrand.Trezor -> R.drawable.trezor_device
}

fun ViewBinding.context(): Context = root.context

fun Activity.snackbar(text: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(findViewById(android.R.id.content), text, duration).show()
}

fun TwoFactorMethod.getIcon(): Int = when (this) {
    TwoFactorMethod.EMAIL -> R.drawable.ic_2fa_email
    TwoFactorMethod.SMS -> R.drawable.ic_2fa_sms
    TwoFactorMethod.PHONE -> R.drawable.ic_2fa_call
    TwoFactorMethod.AUTHENTICATOR -> R.drawable.ic_2fa_authenticator
    TwoFactorMethod.TELEGRAM -> R.drawable.ic_2fa_sms
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

fun <E: View> Collection<E>.setOnClickListener(onClickListener: (v: View) -> Unit) {
    this.forEach {
        it.setOnClickListener(onClickListener)
    }
}

fun <E: View> Collection<E>.setOnClickListenerIndexed(onClickListener: (i: Int, v: View) -> Unit) {
    this.forEachIndexed { i, e ->
        e.setOnClickListener {
            onClickListener.invoke(i, e)
        }
    }
}

fun CharSequence?.isEmailValid(): Boolean {
    return !TextUtils.isEmpty(this) && android.util.Patterns.EMAIL_ADDRESS.matcher(this ?: "").matches()
}

fun MutableLiveData<String>.string() : String = value ?: ""
fun MutableLiveData<Boolean>.boolean() : Boolean = value ?: false
fun MutableLiveData<Boolean>.toggle() : Boolean = (value?.let { !it } ?: false).also { this.value = it }

fun String.fromHtml(): Spanned {
    return Html.fromHtml(this, Html.FROM_HTML_MODE_COMPACT)
}

inline fun <reified T: Enum<T>> T.next(): T {
    val values = enumValues<T>()
    val nextOrdinal = (ordinal + 1) % values.size
    return values[nextOrdinal]
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