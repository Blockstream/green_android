package com.blockstream.green.data.notifications.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequest(
    val externalCustomerId: String,
    val fcmToken: String,
    val platform: String,
)