package com.blockstream.green.data.json

import kotlinx.serialization.json.Json

public val DefaultJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
    allowStructuredMapKeys = true
    useArrayPolymorphism = false
}
