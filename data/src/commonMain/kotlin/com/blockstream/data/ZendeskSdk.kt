package com.blockstream.data

import com.blockstream.data.data.SupportData
import kotlinx.serialization.Serializable

@Serializable
enum class SupportType(val zendeskValue: String) {
    INCIDENT("incident"), FEEDBACK("feedback"),
}

open class ZendeskSdk {
    open val appVersion: String = ""

    open val isAvailable = false

    open suspend fun submitNewTicket(
        type: SupportType,
        subject: String,
        email: String,
        message: String,
        supportData: SupportData,
        autoRetry: Boolean = true
    ): Boolean {
        return false
    }
}
