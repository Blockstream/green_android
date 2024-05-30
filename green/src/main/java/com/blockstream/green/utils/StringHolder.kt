package com.blockstream.green.utils

import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// An extension to mikepenz's StringHolder handling hashCode
// helping with animating data classes
@Parcelize
data class StringHolder constructor(
    val hashCode: Long,
    val text: String?
) : com.mikepenz.fastadapter.ui.utils.StringHolder(text), Parcelable {

    constructor(context: Context, intRes: Int): this(context.getString(intRes).hashCode().toLong(), context.getString(intRes))

    constructor(textString: String? = null) : this(textString.hashCode().toLong(), textString)
}