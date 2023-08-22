package com.blockstream.base

import com.blockstream.common.gdk.data.Network

open class ZendeskSdk {
    var appVersion: String? = null

    open val isAvailable = false

    open fun submitNewTicket(
        subject: String?,
        email: String,
        message: String,
        error: String,
        throwable: Throwable?,
        network: Network?,
        hw: String? // until GdkSession moves to :common and can pass session here
    ) {
    }
}

fun Network.zendeskSecurityPolicy() = when {
    isSinglesig -> "singlesig__green_"
    isMultisig -> "multisig_shield__green_"
    isLightning -> "lightning__green_"
    else -> ""
}
