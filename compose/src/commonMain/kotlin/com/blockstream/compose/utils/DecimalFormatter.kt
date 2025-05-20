package com.blockstream.compose.utils

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue


class DecimalFormatter(
    val decimalSeparator: Char,
    val groupingSeparator: Char
) {
    fun cleanup(input: TextFieldValue): TextFieldValue {
        val originalSize = input.text.length
        val cleanup = cleanup(input.text)
        val newSize = cleanup.length

        return input.copy(
            text = cleanup,
            selection = if (originalSize != newSize) TextRange(newSize) else input.selection
        )
    }

    fun cleanup(input: String): String {
        if (input == decimalSeparator.toString() || input == groupingSeparator.toString()) {
            return "0$decimalSeparator"
        }

        if (input.matches("\\D".toRegex())) return ""
        if (input.matches("0+".toRegex())) return "0"

        val sb = StringBuilder()

        var hasDecimalSep = false

        for (char in input) {
            if (char.isDigit()) {
                sb.append(char)
                continue
            }
            if (char == decimalSeparator && !hasDecimalSep && sb.isNotEmpty()) {
                sb.append(char)
                hasDecimalSep = true
            }
        }

        return sb.toString()
    }

    fun formatForVisual(input: String): String {

        val split = input.split(decimalSeparator)

        val intPart = split[0]
            .reversed()
            .chunked(3)
            .joinToString(separator = groupingSeparator.toString())
            .reversed()

        val fractionPart = split.getOrNull(1)

        return if (fractionPart == null) intPart else intPart + decimalSeparator + fractionPart
    }
}