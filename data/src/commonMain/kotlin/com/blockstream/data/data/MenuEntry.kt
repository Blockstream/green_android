package com.blockstream.data.data

import kotlinx.serialization.Serializable

@Serializable
data class MenuEntry constructor(
    val key: Int = 0,
    val title: String,
    val iconRes: String? = null
)

@Serializable
data class MenuEntryList constructor(
    val list: List<MenuEntry>
)