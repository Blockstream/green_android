package com.blockstream.common.data

import com.blockstream.common.events.Event


data class WalletSettings(val title: String, val subtitle: String)


sealed class WalletSetting{
    data object Logout : WalletSetting()
    data class Text(val title: String?= null, val message: String? = null) : WalletSetting()
    data class LearnMore(val event: Event): WalletSetting()
    data class DenominationExchangeRate(val unit: String, val currency: String, val exchange: String) : WalletSetting()
    data class ArchivedAccounts(val size: Int) : WalletSetting()
    data object WatchOnly : WalletSetting()
    data object SetupEmailRecovery : WalletSetting()
    data object RequestRecoveryTransactions : WalletSetting()
    data class RecoveryTransactionEmails(val enabled: Boolean) : WalletSetting()
    data object ChangePin : WalletSetting()
    data class LoginWithBiometrics(val enabled: Boolean, val canEnable: Boolean) : WalletSetting()
    data object TwoFactorAuthentication : WalletSetting()
    data class PgpKey(val enabled: Boolean) : WalletSetting()
    data class AutologoutTimeout(val timeout: Int) : WalletSetting()
    data object RecoveryPhrase : WalletSetting()
    data class Version(val version: String) : WalletSetting()
    data object Support : WalletSetting()
}
