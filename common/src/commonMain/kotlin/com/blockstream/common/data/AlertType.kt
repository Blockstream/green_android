package com.blockstream.common.data

import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.data.TwoFactorReset

sealed class AlertType {
    data class SystemMessage(val network: Network, val message: String) : AlertType()
    data class Dispute2FA(val network: Network, val twoFactorReset: TwoFactorReset) : AlertType()
    data class Reset2FA(val network: Network, val twoFactorReset: TwoFactorReset) : AlertType()
    data object TestnetWarning : AlertType()
    data object EphemeralBip39 : AlertType()
    data class RecoveryIsUnconfirmed(val withCloseButton: Boolean) : AlertType()
    data class Banner(val banner: com.blockstream.common.data.Banner) : AlertType()
    data object FailedNetworkLogin : AlertType()
    data class LspStatus(val maintenance: Boolean) : AlertType()
}
