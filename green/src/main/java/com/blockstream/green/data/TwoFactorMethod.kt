package com.blockstream.green.data


enum class TwoFactorMethod(val gdkType: String) {
    EMAIL("email"),
    SMS("sms"),
    PHONE("phone"),
    AUTHENTICATOR("gauth");

    override fun toString(): String = gdkType
}
