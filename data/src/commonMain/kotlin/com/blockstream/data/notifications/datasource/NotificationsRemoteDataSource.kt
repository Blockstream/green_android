package com.blockstream.data.notifications.datasource

import com.blockstream.data.GreenWebhooksHttpClient
import com.blockstream.data.notifications.models.RegisterDeviceRequest
import com.blockstream.network.NetworkResponse

class NotificationsRemoteDataSource(
    private val client: GreenWebhooksHttpClient
) {
    suspend fun registerDevice(request: RegisterDeviceRequest): NetworkResponse<Unit> {
        return client.post("/register-device", request)
    }
}