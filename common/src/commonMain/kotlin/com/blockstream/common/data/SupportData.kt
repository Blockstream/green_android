package com.blockstream.common.data

import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.Network
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class SupportData(
    val subject: String? = null,
    val error: String? = null,
    val supportId: String? = null,
    val paymentHash: String? = null,
    val zendeskSecurityPolicy: String? = null,
    val zendeskHardwareWallet: String? = null
) : Parcelable {

    fun withGdkLogs(session: GdkSession?): SupportData =
        copy(error = listOfNotNull(error, session?.logs).takeIf { it.isNotEmpty() }?.joinToString("\n------------------\n"))

    companion object {

        fun create(
            subject: String? = null,
            throwable: Throwable? = null,
            paymentHash: String? = null,
            network: Network? = null,
            session: GdkSession? = null
        ): SupportData {
            return SupportData(
                subject = subject,
                error = throwable?.message,
                supportId = session?.supportId(),
                paymentHash = paymentHash,
                zendeskSecurityPolicy = network?.zendeskValue,
                zendeskHardwareWallet = session?.gdkHwWallet?.model?.zendeskValue,
            )
        }
    }
}