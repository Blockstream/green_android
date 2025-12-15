package com.blockstream.data.gdk.device

class BlindingFactorsResult constructor(
    val assetblinders: MutableList<String>,
    val amountblinders: MutableList<String>
) {
    constructor(capacity: Int) : this(
        assetblinders = ArrayList(capacity),
        amountblinders = ArrayList(capacity)
    )

    fun append(assetblinder: String, amountblinder: String) {
        assetblinders.add(assetblinder)
        amountblinders.add(amountblinder)
    }
}