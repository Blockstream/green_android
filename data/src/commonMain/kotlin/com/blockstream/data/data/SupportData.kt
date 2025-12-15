package com.blockstream.data.data

import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.GreenJson
import com.blockstream.data.gdk.data.Network
import kotlinx.serialization.Serializable

@Serializable
data class SupportData(
    val subject: String? = null,
    val error: String? = null,
    val supportId: String? = null,
    val paymentHash: String? = null,
    val zendeskSecurityPolicy: String? = null,
    val zendeskHardwareWallet: String? = null
) : GreenJson<SupportData>() {

    override fun kSerializer() = serializer()

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