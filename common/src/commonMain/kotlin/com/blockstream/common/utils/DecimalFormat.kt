package com.blockstream.common.utils

const val GDK_LOCALE = "en_US"
expect class DecimalFormat(locale: String?) {
    var minimumFractionDigits: Int
    var maximumFractionDigits: Int
    var isGroupingUsed: Boolean
    var decimalSeparator: Char?
    var groupingSeparator: Char?
    var isDecimalSeparatorAlwaysShown: Boolean
    fun format(double: Double): String?
    fun format(int: Int): String?
    fun format(any: Any): String?
    fun parseTo(input: String, format: DecimalFormat): Pair<String, Double>?
}