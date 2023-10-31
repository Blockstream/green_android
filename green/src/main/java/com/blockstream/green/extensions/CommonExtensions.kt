package com.blockstream.green.extensions

import com.blockstream.common.views.Color
import com.blockstream.green.R


fun Color.resource(): Int = when (this) {
    Color.HIGH -> R.color.color_on_surface_emphasis_high
    Color.MEDIUM -> R.color.color_on_surface_emphasis_medium
    Color.LOW -> R.color.color_on_surface_emphasis_low
    Color.GREEN -> R.color.brand_green
    Color.ORANGE -> R.color.warning
    Color.RED -> R.color.error
}