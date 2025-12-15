package com.blockstream.data.meld.models

import kotlinx.serialization.Serializable

@Serializable
data class Country(
    val countryCode: String,
    val name: String,
    val regions: List<com.blockstream.data.meld.models.Region>? = emptyList(),
    val flagEmoji: String = ""
)

@Serializable
data class Region(
    val regionCode: String,
    val name: String
)