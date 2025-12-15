package com.blockstream.data.data

import kotlinx.serialization.Serializable

@Serializable
enum class PopTo {
    Root, Receive, Transact, OnOffRamps
}