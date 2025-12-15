package com.blockstream.data.gdk.events

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
data class JadeGenuineCheck(val jadeId: String) : GreenJson<JadeGenuineCheck>() {
    override fun kSerializer(): KSerializer<JadeGenuineCheck> = serializer()
}