package com.blockstream.green.adapters


import android.widget.CompoundButton
import androidx.databinding.*
import com.blockstream.green.views.GreenSwitch


@BindingMethods(
    BindingMethod(
        type = GreenSwitch::class,
        attribute = "android:onCheckedChanged",
        method = "setOnCheckedChangeListener"
    )
)
@InverseBindingMethods(
    InverseBindingMethod(
        type = GreenSwitch::class,
        attribute = "android:checked"
    )
)
object GreenSwitchBindingAdapter {
    @BindingAdapter("android:checked")
    @JvmStatic fun setChecked(view: GreenSwitch, checked: Boolean) {
        if (view.isChecked != checked) {
            view.isChecked = checked
        }
    }

    @BindingAdapter(
        value = ["android:onCheckedChanged", "android:checkedAttrChanged"],
        requireAll = false
    )
    @JvmStatic fun setListeners(
        view: GreenSwitch,
        listener: CompoundButton.OnCheckedChangeListener?,
        attrChange: InverseBindingListener?
    ) {
        if (attrChange == null) {
            view.setOnCheckedChangeListener(listener)
        } else {
            view.setOnCheckedChangeListener { buttonView, isChecked ->
                listener?.onCheckedChanged(buttonView, isChecked)
                attrChange.onChange()
            }
        }
    }

    @BindingAdapter("invalid")
    @JvmStatic fun setInvalid(view: GreenSwitch, invalid: Boolean) {
        if (view.isInvalid() != invalid) {
            view.setInvalid(invalid)
        }
    }
}