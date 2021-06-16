package com.blockstream.green.utils

// An extension to mikepenz's StringHolder handling hashCode
// helping with animating data classes
class StringHolder() : com.mikepenz.fastadapter.ui.utils.StringHolder(null) {
    constructor(textString: String?) : this() {
        this.textString = textString
    }

    constructor(textRes: Int) : this() {
        this.textRes = textRes
    }

    override fun hashCode(): Int = textString.hashCode() + textRes.hashCode()
}