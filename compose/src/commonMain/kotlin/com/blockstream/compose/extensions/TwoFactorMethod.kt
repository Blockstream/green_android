package com.blockstream.compose.extensions

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_authenticator_app
import blockstream_green.common.generated.resources.id_call
import blockstream_green.common.generated.resources.id_email
import blockstream_green.common.generated.resources.id_sms
import blockstream_green.common.generated.resources.id_telegram
import com.blockstream.data.data.TwoFactorMethod
import org.jetbrains.compose.resources.StringResource

val TwoFactorMethod.localized: StringResource
    get() = when (this) {
        TwoFactorMethod.EMAIL -> Res.string.id_email
        TwoFactorMethod.SMS -> Res.string.id_sms
        TwoFactorMethod.PHONE -> Res.string.id_call
        TwoFactorMethod.AUTHENTICATOR -> Res.string.id_authenticator_app
        TwoFactorMethod.TELEGRAM -> Res.string.id_telegram
    }