package com.blockstream.data.data

import kotlinx.serialization.Serializable

@Serializable
enum class TwoFactorMethod(val gdkType: String) {
    EMAIL("email"),
    SMS("sms"),
    PHONE("phone"),
    AUTHENTICATOR("gauth"),
    TELEGRAM("telegram");

    override fun toString(): String = gdkType

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
