package com.blockstream.common.utils

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.decimalSeparator
import platform.Foundation.systemLocale

actual class DecimalFormat actual constructor(private val locale: String?) {
    actual var minimumFractionDigits: Int = 0
    actual var maximumFractionDigits: Int = 0
    actual var isGroupingUsed: Boolean = false
    actual var decimalSeparator: Char? = null
    actual var groupingSeparator: Char? = null
    actual var isDecimalSeparatorAlwaysShown: Boolean = false

    private fun decimalFormat(): NSNumberFormatter {
        val formatter = NSNumberFormatter()
        formatter.minimumFractionDigits = minimumFractionDigits.toULong()
        formatter.maximumFractionDigits = maximumFractionDigits.toULong()
        formatter.numberStyle = NSNumberFormatterDecimalStyle ///1u // NSNumberFormatterDecimalStyle// 1u //Decimal

        formatter.locale = locale?.let { NSLocale(localeIdentifier = it) } ?: NSLocale.systemLocale()

        decimalSeparator?.also {
            formatter.setDecimalSeparator(it.toString())
        }

        groupingSeparator?.also {
            formatter.setGroupingSeparator(it.toString())
        }

        return formatter
    }
    actual fun format(double: Double): String? {
        return decimalFormat().stringFromNumber(NSNumber(double))
    }

    actual fun format(int: Int): String? {
        return decimalFormat().stringFromNumber(NSNumber(int))
    }

    actual fun format(any: Any): String? {
        return decimalFormat().stringForObjectValue(any)
    }

    actual fun parseTo(
        input: String,
        format: DecimalFormat
    ): Pair<String, Double>? {
        return decimalFormat().numberFromString(input)?.let {
            format.format(it)!! to it.doubleValue()
        }
    }

    actual companion object {
        actual val DecimalSeparator: String
            get() = NSLocale.systemLocale().decimalSeparator()
    }
}