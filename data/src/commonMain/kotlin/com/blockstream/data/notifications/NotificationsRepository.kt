package com.blockstream.data.notifications

import com.blockstream.data.notifications.datasource.NotificationsRemoteDataSource
import com.blockstream.data.notifications.models.RegisterDeviceRequest
import com.blockstream.jade.Loggable
import com.blockstream.network.NetworkResponse

class NotificationsRepository(
    private val notificationsRemoteDataSource: NotificationsRemoteDataSource,
) {
    suspend fun registerDevice(request: RegisterDeviceRequest): NetworkResponse<Any> {
        logger.d { "registerDevice: $request" }
        return notificationsRemoteDataSource.registerDevice(request)
    }

    companion object : Loggable()
}
