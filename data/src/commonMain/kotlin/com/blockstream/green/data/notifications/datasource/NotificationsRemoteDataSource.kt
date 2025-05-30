package com.blockstream.green.data.notifications.datasource

import com.blockstream.green.data.GreenWebhooksHttpClient
import com.blockstream.green.data.notifications.models.RegisterDeviceRequest
import com.blockstream.green.network.NetworkResponse

class NotificationsRemoteDataSource(
    private val client: GreenWebhooksHttpClient
) {
    suspend fun registerDevice(request: RegisterDeviceRequest): NetworkResponse<Unit> {
        return client.post("/register-device", request)
    }
}