package com.blockstream.green.utils

import android.os.Parcelable
import android.text.Spanned
import android.widget.TextView
import androidx.core.view.isVisible
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

// An extension to mikepenz's StringHolder handling hashCode
// helping with animating data classes
@Parcelize
data class StringHolder constructor(
    val hashCode: Long = 0,
    @IgnoredOnParcel
    val spannedString: Spanned? = null
) : com.mikepenz.fastadapter.ui.utils.StringHolder(null), Parcelable {

    constructor(textString: String?) : this(textString.hashCode().toLong()) {
        @Suppress("INVISIBLE_SETTER_FROM_DERIVED")
        this.textString = textString
    }

    constructor(spannedString: Spanned) : this(spannedString.toString().hashCode().toLong(), spannedString) {
        @Suppress("INVISIBLE_SETTER_FROM_DERIVED")
        this.textString = spannedString.toString()
    }

    constructor(textRes: Int) : this(textRes.toLong()) {
        @Suppress("INVISIBLE_SETTER_FROM_DERIVED")
        this.textRes = textRes
    }

    override fun applyToOrHide(textView: TextView?): Boolean {
        return if(spannedString != null){
            textView?.isVisible = true
            textView?.text = spannedString
            true
        }else {
            super.applyToOrHide(textView)
        }
    }
}