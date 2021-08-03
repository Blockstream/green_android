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

    // Note that 2FA config for telegram (and soon, u2f) may not be present in the 2fa json.
    // Unlike the existing methods which are generally always present, the new methods may not be, and if not, should be assumed to be  disabled.
    @SerialName("telegram") val telegram: TwoFactorMethodConfig = TwoFactorMethodConfig(),

    @SerialName("limits") val limits: Balance,
    @SerialName("twofactor_reset") val twoFactorReset: TwoFactorReset,
)