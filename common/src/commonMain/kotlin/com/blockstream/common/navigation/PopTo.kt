package com.blockstream.common.navigation

import kotlinx.serialization.Serializable

@Serializable
enum class PopTo {
    Root, Receive, OnOffRamps
}