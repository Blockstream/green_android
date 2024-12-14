package com.blockstream.common.gdk.events

import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
data class JadeGenuineCheck(val jadeId: String): GreenJson<JadeGenuineCheck>() {
    override fun kSerializer(): KSerializer<JadeGenuineCheck> = serializer()
}