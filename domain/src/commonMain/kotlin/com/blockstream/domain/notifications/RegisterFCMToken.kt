package com.blockstream.domain.notifications

import com.blockstream.data.notifications.NotificationsRepository
import com.blockstream.data.notifications.models.RegisterDeviceRequest
import com.blockstream.domain.base.NetworkResultUseCase
import com.blockstream.network.NetworkResponse

class RegisterFCMToken(
    private val notificationsRepository: NotificationsRepository,
) : NetworkResultUseCase<RegisterFCMToken.Params, Any>() {
    override suspend fun doWork(params: Params): NetworkResponse<Any> {
        val request = RegisterDeviceRequest(
            externalCustomerId = params.externalCustomerId,
            fcmToken = params.fcmToken,
            platform = params.platform,
        )
        val response = notificationsRepository.registerDevice(request)
        return response
    }

    data class Params(
        val externalCustomerId: String,
        val fcmToken: String,
        val platform: String = "android",
    )
}