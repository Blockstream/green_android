package com.blockstream.green.utils

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// An extension to mikepenz's StringHolder handling hashCode
// helping with animating data classes
@Parcelize
data class StringHolder constructor(val hashCode: Long = 0) : com.mikepenz.fastadapter.ui.utils.StringHolder(null), Parcelable {
    constructor(textString: String?) : this(textString.hashCode().toLong()) {
        this.textString = textString
    }

    constructor(textRes: Int) : this(textRes.toLong()) {
        this.textRes = textRes
    }
}