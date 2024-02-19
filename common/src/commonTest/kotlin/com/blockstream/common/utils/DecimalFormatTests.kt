package com.blockstream.common.utils

import com.blockstream.common.BTC_UNIT
import com.blockstream.common.SATOSHI_UNIT
import kotlin.test.Test
import kotlin.test.assertEquals

class DecimalFormatTests {
    @Test
    fun `test en_US`() {
        DecimalFormat("en_US").apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 3
            isGroupingUsed = true
        }.apply {
            assertEquals("123.00", format(123))
            assertEquals("123.10", format(123.1))
            assertEquals("123.12", format(123.12))
            assertEquals("123.123", format(123.123))
            assertEquals("1,123.123", format(1123.123))
        }
    }

    @Test
    fun `test italy`() {
        DecimalFormat("it").apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 3
            isGroupingUsed = true
        }.apply {
            assertEquals("123,00", format(123))
            assertEquals("123,10", format(123.1))
            assertEquals("123,12", format(123.12))
            assertEquals("123,123", format(123.123))
            assertEquals("1.123,123", format(1123.123))
        }
    }

    @Test
    fun `test parse`() {
        DecimalFormat("it").apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 3
            isGroupingUsed = true
        }.apply {
            gdkNumberFormat(getDecimals(BTC_UNIT)).also {
                assertEquals("123", parseTo("123", it)!!.first)
                assertEquals("123.123", parseTo("123,123", it)!!.first)
                assertEquals("0.12345678", parseTo(",12345678", it)!!.first)
            }

            gdkNumberFormat(getDecimals(SATOSHI_UNIT)).also {
                assertEquals("123", parseTo("123", it)!!.first)
                assertEquals("123", parseTo("123,123", it)!!.first)
            }
        }
    }

    @Test
    fun `test separators`() {
        DecimalFormat(null).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            isGroupingUsed = true
            decimalSeparator = '-'
            groupingSeparator = '_'
        }.apply {
            assertEquals("1_123-12", format(1123.12))
            assertEquals("1_123-12", format(1123.12000))
        }
    }
}