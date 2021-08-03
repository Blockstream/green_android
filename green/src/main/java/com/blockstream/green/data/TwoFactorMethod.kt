package com.blockstream.green.data

import com.blockstream.green.R


enum class TwoFactorMethod(val gdkType: String) {
    EMAIL("email"),
    SMS("sms"),
    PHONE("phone"),
    AUTHENTICATOR("gauth"),
    TELEGRAM("telegram");

    override fun toString(): String = gdkType

    fun getIcon(): Int = when (this) {
        EMAIL -> R.drawable.ic_2fa_email
        SMS -> R.drawable.ic_2fa_sms
        PHONE -> R.drawable.ic_2fa_call
        AUTHENTICATOR -> R.drawable.ic_2fa_authenticator
        TELEGRAM -> R.drawable.ic_2fa_sms
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
