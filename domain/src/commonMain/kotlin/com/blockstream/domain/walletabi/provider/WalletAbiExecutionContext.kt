package com.blockstream.domain.walletabi.provider

import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.domain.walletabi.request.WalletAbiNetwork

data class WalletAbiExecutionContext(
    val session: GdkSession,
    val requestNetwork: WalletAbiNetwork,
    val accounts: List<Account>,
    val primaryAccount: Account,
    val signerBackend: WalletAbiSignerBackend,
)

sealed interface WalletAbiSignerBackend {
    data object Software : WalletAbiSignerBackend

    data class Jade(
        val deviceId: String,
        val deviceName: String,
    ) : WalletAbiSignerBackend
}

class WalletAbiExecutionContextException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
