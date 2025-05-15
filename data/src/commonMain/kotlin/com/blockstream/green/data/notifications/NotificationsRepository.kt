package com.blockstream.green.data.notifications

import com.blockstream.green.data.notifications.datasource.NotificationsRemoteDataSource
import com.blockstream.green.data.notifications.models.RegisterDeviceRequest
import com.blockstream.green.network.NetworkResponse

class NotificationsRepository(
    private val notificationsRemoteDataSource: NotificationsRemoteDataSource,
) {
    suspend fun registerDevice(request: RegisterDeviceRequest): NetworkResponse<Any> {
        return notificationsRemoteDataSource.registerDevice(request)
    }
}
