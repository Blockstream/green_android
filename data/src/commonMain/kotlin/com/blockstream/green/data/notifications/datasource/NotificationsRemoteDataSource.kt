package com.blockstream.green.data.notifications.datasource

import com.blockstream.green.data.GreenWebooksHttpClient
import com.blockstream.green.data.notifications.models.RegisterDeviceRequest
import com.blockstream.green.network.NetworkResponse

class NotificationsRemoteDataSource(
    private val client: GreenWebooksHttpClient
) {
    suspend fun registerDevice(request: RegisterDeviceRequest): NetworkResponse<Unit> {
        return client.post("/register-device", request)
    }
}