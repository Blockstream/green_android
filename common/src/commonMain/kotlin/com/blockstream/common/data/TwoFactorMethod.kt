package com.blockstream.common.data

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_authenticator_app
import blockstream_green.common.generated.resources.id_call
import blockstream_green.common.generated.resources.id_email
import blockstream_green.common.generated.resources.id_sms
import blockstream_green.common.generated.resources.id_telegram
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource


@Serializable
enum class TwoFactorMethod(val gdkType: String) {
    EMAIL("email"),
    SMS("sms"),
    PHONE("phone"),
    AUTHENTICATOR("gauth"),
    TELEGRAM("telegram");

    override fun toString(): String = gdkType

    val localized : StringResource
        get() = when(this){
            EMAIL -> Res.string.id_email
            SMS -> Res.string.id_sms
            PHONE -> Res.string.id_call
            AUTHENTICATOR -> Res.string.id_authenticator_app
            TELEGRAM -> Res.string.id_telegram
        }

    companion object {
        fun from(gdkType: String) = when (gdkType) {
            EMAIL.gdkType -> EMAIL
            AUTHENTICATOR.gdkType -> AUTHENTICATOR
            PHONE.gdkType -> PHONE
            TELEGRAM.gdkType -> TELEGRAM
            else -> SMS
        }
    }
}
