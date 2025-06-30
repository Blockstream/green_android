package com.blockstream.common.utils

import java.text.DateFormat
import java.util.Date
import kotlin.time.Instant

private fun Instant.toDate() = Date(toEpochMilliseconds())

actual fun Instant.formatMediumOnlyDate(): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(this.toDate())

actual fun Instant.formatMediumWithTime(): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(this.toDate())

actual fun Instant.formatFullWithTime(): String =
    DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT).format(this.toDate())