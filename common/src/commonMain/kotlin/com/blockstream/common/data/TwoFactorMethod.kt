package com.blockstream.common.data


enum class TwoFactorMethod(val gdkType: String) {
    EMAIL("email"),
    SMS("sms"),
    PHONE("phone"),
    AUTHENTICATOR("gauth"),
    TELEGRAM("telegram");

    override fun toString(): String = gdkType

    val localized : String
        get() = when(this){
            EMAIL -> "id_email"
            SMS -> "id_sms"
            PHONE -> "id_call"
            AUTHENTICATOR -> "id_authenticator_app"
            TELEGRAM -> "id_telegram"
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
