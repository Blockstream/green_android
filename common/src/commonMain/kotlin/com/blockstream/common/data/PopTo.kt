package com.blockstream.common.data

import kotlinx.serialization.Serializable

@Serializable
enum class PopTo {
    Root, Receive, Transact, OnOffRamps
}