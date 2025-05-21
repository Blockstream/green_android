package com.blockstream.common.data

import com.blockstream.common.gdk.data.Network
import com.blockstream.ui.events.Event

sealed class WalletSetting{
    data object Logout : WalletSetting()
    data class Text(val title: String? = null, val message: String? = null) : WalletSetting()
    data class LearnMore(val event: Event): WalletSetting()
    data class ButtonEvent(val title: String, val event: Event): WalletSetting()
    data class DenominationExchangeRate(val unit: String, val currency: String, val exchange: String) : WalletSetting()
    data object ArchivedAccounts : WalletSetting()
    data object WatchOnly : WalletSetting()
    data object SetupEmailRecovery : WalletSetting()
    data class RequestRecovery(val network: Network) : WalletSetting()
    data object RequestRecoveryTransactions : WalletSetting()
    data class RecoveryTransactionEmails(val enabled: Boolean) : WalletSetting()
    data object ChangePin : WalletSetting()
    data class Lightning(val enabled: Boolean) : WalletSetting()
    data class LoginWithBiometrics(val enabled: Boolean, val canEnable: Boolean) : WalletSetting()
    data object TwoFactorAuthentication : WalletSetting()
    data class PgpKey(val enabled: Boolean) : WalletSetting()
    data class AutoLogoutTimeout(val timeout: Int) : WalletSetting()
    data object JadeGenuineCheck : WalletSetting()
    data object RecoveryPhrase : WalletSetting()
    data class Version(val version: String) : WalletSetting()
    data class TwoFactorMethod(
        val method: com.blockstream.common.data.TwoFactorMethod,
        val data : String?,
        val enabled: Boolean
    ) : WalletSetting()

    data class TwoFactorBucket(
        val title: String,
        val subtitle : String,
        val enabled: Boolean,
        val bucket: Int
    ) : WalletSetting()

    data class TwoFactorThreshold(
        val subtitle: String
    ) : WalletSetting()

    data class CopyAmpId(
        val title: String,
        val receivingId: String,
    ) : WalletSetting()

    data object CreateAmpAccount : WalletSetting()

    data object RenameWallet : WalletSetting()
    data object GetSupport : WalletSetting()
    data object SupportId : WalletSetting()
}
