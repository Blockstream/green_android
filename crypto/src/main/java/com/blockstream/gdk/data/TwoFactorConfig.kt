package com.blockstream.gdk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TwoFactorConfig(
    @SerialName("all_methods") val allMethods: List<String> = listOf(),
    @SerialName("enabled_methods") val enabledMethods: List<String> = listOf(),
    @SerialName("any_enabled") val anyEnabled: Boolean,

    @SerialName("email") val email: TwoFactorMethodConfig,
    @SerialName("sms") val sms: TwoFactorMethodConfig,
    @SerialName("gauth") val gauth: TwoFactorMethodConfig,
    @SerialName("phone") val phone: TwoFactorMethodConfig,

    @SerialName("limits") val limits: Balance,
    @SerialName("twofactor_reset") val twoFactorReset: TwoFactorReset,
) {
    fun getMethod(method: String): TwoFactorMethodConfig {
        return when (method) {
            "email" -> email
            "phone" -> phone
            "gauth" -> gauth
            else -> sms
        }
    }
}
