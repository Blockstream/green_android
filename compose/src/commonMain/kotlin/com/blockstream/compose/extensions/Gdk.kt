package com.blockstream.compose.extensions

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_authenticator_app
import blockstream_green.common.generated.resources.id_call
import blockstream_green.common.generated.resources.id_email
import blockstream_green.common.generated.resources.id_sms
import blockstream_green.common.generated.resources.id_telegram
import org.jetbrains.compose.resources.StringResource

fun String.twoFactorMethodsLocalized(): StringResource = when (this) {
    "email" -> Res.string.id_email
    "phone" -> Res.string.id_call
    "telegram" -> Res.string.id_telegram
    "gauth" -> Res.string.id_authenticator_app
    else -> Res.string.id_sms
}

fun List<String>.twoFactorMethodsLocalized(): List<StringResource> = map {
    it.twoFactorMethodsLocalized()
}