package com.blockstream.common.gdk.data

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.gdk.GreenJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
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
): GreenJson<TwoFactorConfig>(), Parcelable {
    override fun kSerializer() = serializer()

    companion object{
        // Emtpy object in case of 2FA bug in GDK
        val empty = TwoFactorConfig(
            anyEnabled = false,
            email = TwoFactorMethodConfig(),
            sms = TwoFactorMethodConfig(),
            gauth = TwoFactorMethodConfig(),
            phone = TwoFactorMethodConfig(),
            limits = Balance(
                satoshi = 0,
                bits = "",
                btc = "",
                fiatCurrency = "",
                mbtc = "",
                sats = "",
                ubtc = ""
            ),
            twoFactorReset = TwoFactorReset()

        )
    }
}