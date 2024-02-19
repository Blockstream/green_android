package com.blockstream.compose.utils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

fun Date.formatMediumOnlyDate(): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(this)

fun Date.formatMediumWithTime(): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(this)


private val FullWithTime by lazy { SimpleDateFormat("MMMM dd, YYYY HH:mm") }
fun Date.formatFullWithTime(): String = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT).format(this)

fun Date.formatAuto(): String =
    if ((this.time + 24 * 60 * 60 * 1000) > (System.currentTimeMillis())) {
        formatMediumWithTime()
    } else {
        formatMediumOnlyDate()
    }