package com.blockstream.domain.walletabi.provider

import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.Network
import com.blockstream.domain.walletabi.request.WalletAbiNetwork

interface WalletAbiExecutionContextResolving {
    suspend fun resolveDirect(
        session: GdkSession,
        requestNetwork: WalletAbiNetwork,
        preferredAccountId: String? = null,
    ): WalletAbiExecutionContext
}

class WalletAbiExecutionContextResolver : WalletAbiExecutionContextResolving {
    override suspend fun resolveDirect(
        session: GdkSession,
        requestNetwork: WalletAbiNetwork,
        preferredAccountId: String?,
    ): WalletAbiExecutionContext {
        requireSoftwareSigner(session)

        fun currentAccounts() = (session.accounts.value + session.allAccounts.value)
            .distinctBy { it.id }
            .filter { account ->
                account.isLiquid &&
                    !account.isLightning &&
                    account.network.matchesWalletAbiNetwork(requestNetwork)
            }

        var accounts = currentAccounts()
        if (accounts.isEmpty()) {
            session.updateAccountsAndBalances(refresh = true).join()
            accounts = currentAccounts()
        }
        if (accounts.isEmpty()) {
            throw WalletAbiExecutionContextException(
                "Wallet ABI found no Liquid account for '${requestNetwork.wireValue}'",
            )
        }

        val primaryAccount = preferredAccountId?.let { requestedId ->
            accounts.firstOrNull { it.id == requestedId }
        } ?: session.activeAccount.value?.takeIf { active ->
            accounts.any { it.id == active.id }
        } ?: accounts.first()

        return WalletAbiExecutionContext(
            session = session,
            requestNetwork = requestNetwork,
            accounts = accounts,
            primaryAccount = primaryAccount,
        )
    }
}

suspend fun requireSoftwareSigner(session: GdkSession) {
    if (!session.isConnected) {
        throw WalletAbiExecutionContextException("Wallet ABI requires a connected session")
    }
    if (session.isHardwareWallet) {
        throw WalletAbiExecutionContextException("Wallet ABI v1 supports only mnemonic-backed wallets")
    }

    val mnemonic = try {
        session.getCredentials().mnemonic
    } catch (error: Exception) {
        throw WalletAbiExecutionContextException(
            message = "Wallet ABI failed to read wallet credentials: ${error.message}",
            cause = error,
        )
    }

    if (mnemonic.isNullOrBlank()) {
        throw WalletAbiExecutionContextException("Wallet ABI requires mnemonic credentials")
    }
}

fun Account.walletAbiAccountIndex(): UInt {
    derivationPath
        ?.lastOrNull()
        ?.let { child ->
            val normalized = if (child >= WALLET_ABI_HARDENED_BIT_LONG) {
                child - WALLET_ABI_HARDENED_BIT_LONG
            } else {
                child
            }
            if (normalized in 0..UInt.MAX_VALUE.toLong()) {
                return normalized.toUInt()
            }
        }

    val fallbackIndex = if (type.isSinglesig()) {
        pointer / 16L
    } else {
        pointer
    }.coerceAtLeast(0L)

    return fallbackIndex.coerceAtMost(UInt.MAX_VALUE.toLong()).toUInt()
}

private fun Network.matchesWalletAbiNetwork(requestNetwork: WalletAbiNetwork): Boolean {
    return when (requestNetwork) {
        WalletAbiNetwork.LIQUID -> isLiquidMainnet
        WalletAbiNetwork.TESTNET_LIQUID -> isLiquidTestnet && !isDevelopment
        WalletAbiNetwork.LOCALTEST_LIQUID -> isLiquid && isDevelopment
    }
}

private const val WALLET_ABI_HARDENED_BIT_LONG = 0x8000_0000L
