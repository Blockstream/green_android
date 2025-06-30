package com.blockstream.common.gdk.events

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlin.time.Clock

// deviceId creates uniqueness, else just timestamp can be reversed
@Serializable
data class GenericEvent constructor(val deviceId: String, val timestamp: Long = Clock.System.now().toEpochMilliseconds()) :
    GreenJson<GenericEvent>() {
    override fun kSerializer(): KSerializer<GenericEvent> = serializer()
}