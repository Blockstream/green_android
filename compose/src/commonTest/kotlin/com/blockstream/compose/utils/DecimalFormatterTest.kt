package com.blockstream.compose.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class DecimalFormatterTest {
    private val formatter = DecimalFormatter(decimalSeparator = '.', groupingSeparator = ',')

    @Test
    fun cleanupKeepsOneDecimalSeparatorAndDropsGroupingCharacters() {
        assertEquals("123.45", formatter.cleanup("12,3.4.5"))
    }

    @Test
    fun cleanupMakesStandaloneSeparatorAValidPartialAmount() {
        assertEquals("0.", formatter.cleanup("."))
        assertEquals("0.", formatter.cleanup(","))
    }

    @Test
    fun cleanupCollapsesLeadingZeroes() {
        assertEquals("0", formatter.cleanup("000"))
    }

    @Test
    fun cleanupDropsDigitsBeyondMaxFractionDigits() {
        val fiatFormatter = DecimalFormatter(decimalSeparator = '.', groupingSeparator = ',', maxFractionDigits = 2)

        assertEquals("1.23", fiatFormatter.cleanup("1.234"))
    }
}
