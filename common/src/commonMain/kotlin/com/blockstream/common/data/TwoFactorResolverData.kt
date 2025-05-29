package com.blockstream.common.data

import com.blockstream.common.gdk.data.AuthHandlerStatus
import com.blockstream.common.gdk.data.Network

data class TwoFactorResolverData constructor(
    val methods: List<String>? = null,
    val network: Network? = null,
    val enable2faCallMethod: Boolean = false,
    val authHandlerStatus: AuthHandlerStatus? = null
) {
    companion object {
        fun selectMethod(methods: List<String>) = TwoFactorResolverData(methods = methods)
        fun getCode(
            network: Network,
            enable2faCallMethod: Boolean,
            authHandlerStatus: AuthHandlerStatus
        ) = TwoFactorResolverData(
            network = network,
            enable2faCallMethod = enable2faCallMethod,
            authHandlerStatus = authHandlerStatus
        )
    }

}

