package com.blockstream.green.adapters

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import com.google.android.material.progressindicator.BaseProgressIndicator
import com.google.android.material.textfield.TextInputLayout

@BindingAdapter("isVisible")
fun bindIsVisible(view: View, isVisible: Boolean) {
    view.isVisible = isVisible
}

@BindingAdapter("isGone")
fun bindIsGone(view: View, isGone: Boolean) {
    view.isVisible = !isGone
}

@BindingAdapter("isInvisible")
fun bindIsInvisible(view: View, isInvisible: Boolean) {
    view.isInvisible = isInvisible
}

@BindingAdapter("error")
fun bindError(view: TextInputLayout, error: String?) {
    view.error = error
}

@BindingAdapter("android:src")
fun setImageViewResource(imageView: ImageView, resource: Int) {
    imageView.setImageResource(resource)
}

@BindingAdapter("android:text")
fun setTextResource(textView: TextView, @StringRes resource: Int) {
    if(resource == 0) {
        textView.text = ""
    }else{
        textView.setText(resource)
    }
}

@BindingAdapter("textAsNumber")
fun setTextAsNumber(textView: TextView, number: Int) {
    textView.text = number.toString()
}

@BindingAdapter("indeterminate")
fun setIndeterminate(progressIndicator: BaseProgressIndicator<*>, isIndeterminate: Boolean) {
    val isVisible = progressIndicator.isVisible

    if(!progressIndicator.isIndeterminate && isIndeterminate){
        progressIndicator.isVisible = false
    }

    progressIndicator.isIndeterminate = isIndeterminate
    progressIndicator.isVisible = isVisible
}

@BindingAdapter("progress")
fun setProgress(progressIndicator: BaseProgressIndicator<*>, progress: Int) {
    if(!progressIndicator.isIndeterminate) {
        progressIndicator.setProgressCompat(progress, true)
    }
}
