package com.blockstream.ui.navigation

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface Route {
    val uniqueId: String
    val unique: Boolean
    val makeItRoot: Boolean
}

@Serializable
data class Dialog @OptIn(ExperimentalUuidApi::class) constructor(
    override val uniqueId: String = Uuid.random().toHexString(),
    val title: String? = null,
    val message: String? = null,
    val confirmButtonText: String? = null,
    val dismissButtonText: String? = null,
) : Route {
    override val unique: Boolean = false
    override val makeItRoot: Boolean = false
}