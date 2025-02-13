package com.blockstream.green.data.json

import kotlinx.serialization.json.Json

public val DefaultJson: Json = Json {
    encodeDefaults = true
    isLenient = true
    allowStructuredMapKeys = true
    useArrayPolymorphism = false
}