package com.blockstream.common.lwk

import kotlinx.serialization.Serializable

@Serializable
data class BoltzLimits constructor(
    val limits: Limits,
)

@Serializable
data class Limits(
    val minimal: Long? = null,
    val maximal: Long? = null,
)