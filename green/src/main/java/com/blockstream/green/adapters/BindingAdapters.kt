package com.blockstream.green.adapters

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.blockstream.gdk.data.Device
import com.blockstream.green.data.Banner
import com.blockstream.green.gdk.getIcon
import com.blockstream.green.utils.errorFromResourcesAndGDK
import com.blockstream.green.views.GreenAlertView
import com.google.android.material.progressindicator.BaseProgressIndicator
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputLayout

@BindingAdapter("isVisible")
fun bindIsVisible(view: View, isVisible: Boolean) {
    view.isVisible = isVisible

    // Workaround for bug https://github.com/material-components/material-components-android/issues/1972
    if(view is BaseProgressIndicator<*>){
        view.showAnimationBehavior = BaseProgressIndicator.SHOW_INWARD
        view.hideAnimationBehavior = BaseProgressIndicator.HIDE_OUTWARD
        view.translationZ = 1.0f
    }
}

@BindingAdapter("isGone")
fun bindIsGone(view: View, isGone: Boolean) {
    view.isVisible = !isGone
}

@BindingAdapter("isInvisible")
fun bindIsInvisible(view: View, isInvisible: Boolean) {
    view.isInvisible = isInvisible
}

@BindingAdapter("visibility")
fun bindVisibility(view: View, visibility: Int) {
    // Better to use View constants, but works even with 0, 1, 2
    view.visibility = when (visibility) {
        0 -> View.VISIBLE
        1 -> View.INVISIBLE
        2 -> View.GONE
        else -> visibility
    }
}

@BindingAdapter("banner")
fun bindBanner(view: GreenAlertView, banner: Banner?) {
    if(banner != null) {
        view.title = banner.title ?: ""
        view.message = banner.message ?: ""
        view.setIconVisibility(banner.isWarning)
        view.setMaxLines(3)
    }
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

@BindingAdapter("gdkDevice")
fun setGdkDevice(view: ImageView, device: Device?) {
    view.setImageResource(device?.getIcon() ?: 0)
}

@BindingAdapter("greenDevice")
fun setGreenDevice(view: ImageView, device: com.blockstream.green.devices.Device?) {
    view.setImageResource(device?.getIcon() ?: 0)
}

@BindingAdapter("layoutMarginLeft")
fun setLayoutMarginLeft(view: View, dimen: Float) {
    val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
    layoutParams.leftMargin = dimen.toInt()
    view.layoutParams = layoutParams
}

@BindingAdapter("layoutMarginRight")
fun setLayoutMarginRight(view: View, dimen: Float) {
    val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
    layoutParams.rightMargin = dimen.toInt()
    view.layoutParams = layoutParams
}

@BindingAdapter("layoutMarginBottom")
fun setLayoutMarginBottom(view: View, dimen: Float) {
    val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
    layoutParams.bottomMargin = dimen.toInt()
    view.layoutParams = layoutParams
}

@BindingAdapter("gdkError")
fun setGdkError(textInputLayout: TextInputLayout, error: String?) {
    if(error.isNullOrBlank()){
        textInputLayout.error = null
        textInputLayout.setTag("errorText".hashCode(), null)

        // Restore helper text if existed
        textInputLayout.getTag("helperText".hashCode())?.let {
            if(it is String){
                textInputLayout.helperText = it
            }
        }
    }else{
        if(textInputLayout.editText?.text.isNullOrBlank()){
            // save error text to be used by TextInputWithGdkErrorEditText
            textInputLayout.setTag("errorText".hashCode(), error)
        }else{
            textInputLayout.error = textInputLayout.context.errorFromResourcesAndGDK(error)
        }
    }
}

// Set helper text if errors is null/blank else store it for gdkError to restore it
@BindingAdapter("helperTextWithError")
fun helperTextWithError(textInputLayout: TextInputLayout, text: String?) {
    if(textInputLayout.error.isNullOrBlank()){
        textInputLayout.helperText = text
        textInputLayout.setTag("helperText".hashCode(), null) // clear old helper text
    }else{
        textInputLayout.setTag("helperText".hashCode(), text)
    }
}

@BindingAdapter("gdkError")
fun setGdkError(textView: TextView, error: String?) {
    if(error.isNullOrBlank()){
        textView.isVisible = false
    }else{
        textView.text = textView.context.errorFromResourcesAndGDK(error)
        textView.isVisible = true
    }
}

@InverseBindingAdapter(attribute = "android:value")
fun getSliderValue(slider: Slider) = slider.value

@BindingAdapter("android:valueAttrChanged")
fun setSliderListeners(slider: Slider, attrChange: InverseBindingListener) {
    slider.addOnChangeListener { _, _, _ ->
        attrChange.onChange()
    }
}