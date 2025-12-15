package com.blockstream.compose.extensions

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_custom
import blockstream_green.common.generated.resources.id_high
import blockstream_green.common.generated.resources.id_low
import blockstream_green.common.generated.resources.id_medium
import com.blockstream.data.data.FeePriority

val FeePriority.title
    get() = when (this) {
        is FeePriority.Custom -> Res.string.id_custom
        is FeePriority.Low -> Res.string.id_low
        is FeePriority.Medium -> Res.string.id_medium
        is FeePriority.High -> Res.string.id_high
    }