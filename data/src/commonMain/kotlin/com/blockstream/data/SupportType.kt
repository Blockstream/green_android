package com.blockstream.data

import kotlinx.serialization.Serializable

@Serializable
enum class SupportType(val zendeskValue: String) {
    INCIDENT("incident"), FEEDBACK("feedback"),
}
