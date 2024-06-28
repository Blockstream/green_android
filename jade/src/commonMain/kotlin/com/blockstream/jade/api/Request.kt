package com.blockstream.jade.api

import com.blockstream.jade.TIMEOUT_AUTONOMOUS
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class Request<T, P> : JadeSerializer<T>() {
    abstract val id: String
    abstract val method: String
    abstract val params: P?

    @Transient
    open val timeout: Int = TIMEOUT_AUTONOMOUS
}