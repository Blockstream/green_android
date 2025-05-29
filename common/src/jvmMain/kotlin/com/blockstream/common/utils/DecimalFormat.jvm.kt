package com.blockstream.common.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.ParsePosition
import java.util.Locale

actual class DecimalFormat actual constructor(private val locale: String?) {
    actual var minimumFractionDigits: Int = 0
    actual var maximumFractionDigits: Int = 0
    actual var isGroupingUsed: Boolean = false
    actual var decimalSeparator: Char? = null
    actual var groupingSeparator: Char? = null
    actual var isDecimalSeparatorAlwaysShown: Boolean = false

    private fun decimalFormat(): DecimalFormat {
        val locale = locale?.let { Locale.forLanguageTag(locale) } ?: Locale.getDefault()
        val df = (DecimalFormat.getInstance(locale) as DecimalFormat)
        df.minimumFractionDigits = minimumFractionDigits
        df.maximumFractionDigits = maximumFractionDigits
        df.isDecimalSeparatorAlwaysShown = isDecimalSeparatorAlwaysShown
        df.isGroupingUsed = isGroupingUsed

        if (decimalSeparator != null && groupingSeparator != null) {
            df.decimalFormatSymbols = DecimalFormatSymbols(locale).also {
                it.decimalSeparator = decimalSeparator ?: '.'
                it.groupingSeparator = groupingSeparator ?: ','
            }
        }

        return df
    }

    actual fun format(double: Double): String? {
        return try {
            decimalFormat().format(double)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun format(int: Int): String? {
        return try {
            decimalFormat().format(int)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun format(any: Any): String? {
        return try {
            decimalFormat().format(any)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun parseTo(input: String, format: com.blockstream.common.utils.DecimalFormat): Pair<String, Double>? {
        val position = ParsePosition(0)

        return decimalFormat().parse(input, position).also {
            if (position.index != input.length) {
                throw Exception("id_invalid_amount")
            }
        }?.let {
            format.format(it)!! to it.toDouble()
        }
    }

    actual companion object {
        actual val DecimalSeparator: String
            get() = DecimalFormatSymbols().decimalSeparator.toString()
        actual val GroupingSeparator: String
            get() = DecimalFormatSymbols().groupingSeparator.toString()
    }
}