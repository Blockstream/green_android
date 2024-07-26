package com.blockstream.green.adapters

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import app.rive.runtime.kotlin.RiveAnimationView
import com.blockstream.common.gdk.data.Device
import com.blockstream.common.gdk.device.DeviceInterface
import com.blockstream.green.extensions.stringFromIdentifierOrNull
import com.blockstream.green.gdk.getIcon
import com.google.android.material.progressindicator.BaseProgressIndicator


@BindingAdapter("isVisible")
fun bindIsVisible(view: View, isVisible: Boolean) {
    view.isVisible = isVisible

    // Workaround for bug https://github.com/material-components/material-components-android/issues/1972
    if(view is BaseProgressIndicator<*>){
        view.showAnimationBehavior = BaseProgressIndicator.SHOW_INWARD
        view.hideAnimationBehavior = BaseProgressIndicator.HIDE_OUTWARD
        view.translationZ = 1.0f
    } else if (view is RiveAnimationView) {
        if (isVisible) {
            view.play()
        } else {
            view.stop()
        }
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

@BindingAdapter("android:text")
fun setTextResource(textView: TextView, @StringRes resource: Int) {
    if(resource == 0) {
        textView.text = ""
    }else{
        textView.setText(resource)
    }
}

@BindingAdapter("idText")
fun setIdText(textView: TextView, idText: String?) {
    textView.text = idText?.let { textView.context.stringFromIdentifierOrNull(idText) ?: idText } ?: ""
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
fun setGreenDevice(view: ImageView, device: DeviceInterface?) {
    view.setImageResource((device as? com.blockstream.green.devices.Device)?.getIcon() ?: 0)
}
