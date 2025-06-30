package com.blockstream.common.utils

import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

expect fun Instant.formatMediumOnlyDate(): String
expect fun Instant.formatMediumWithTime(): String
expect fun Instant.formatFullWithTime(): String

fun Instant.formatAuto(): String =
    if (plus(1L.toDuration(DurationUnit.DAYS)) > Clock.System.now()) {
        formatMediumWithTime()
    } else {
        formatMediumOnlyDate()
    }