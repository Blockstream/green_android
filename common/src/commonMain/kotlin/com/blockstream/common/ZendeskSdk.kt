package com.blockstream.common

import com.blockstream.common.data.ErrorReport

open class ZendeskSdk {
    var appVersion: String? = null

    open val isAvailable = false

    open fun submitNewTicket(
        subject: String?,
        email: String,
        message: String,
        errorReport: ErrorReport
    ) { }
}
