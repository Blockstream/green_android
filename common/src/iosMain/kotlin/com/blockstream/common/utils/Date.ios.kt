package com.blockstream.common.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.toNSDate
import platform.Foundation.NSDateFormatter

private val MediumOnlyDate by lazy {
    NSDateFormatter().also {
        it.dateFormat = "MMMM dd, YYYY"
    }
}

private val MediumWithTime by lazy {
    NSDateFormatter().also {
        it.dateFormat = "MMMM dd, YYYY HH:mm"
    }
}

private val FullWithTime by lazy {
    NSDateFormatter().also {
        it.dateFormat = "EE MMMM dd, YYYY HH:mm"
    }
}

actual fun Instant.formatMediumOnlyDate(): String = MediumOnlyDate.stringFromDate(toNSDate())

actual fun Instant.formatMediumWithTime(): String = MediumWithTime.stringFromDate(toNSDate())

actual fun Instant.formatFullWithTime(): String = FullWithTime.stringFromDate(toNSDate())