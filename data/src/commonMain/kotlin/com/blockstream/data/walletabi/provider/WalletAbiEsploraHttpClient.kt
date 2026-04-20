package com.blockstream.data.walletabi.provider

import com.blockstream.data.config.AppInfo
import com.blockstream.data.gdk.data.Network as GdkNetwork
import com.blockstream.network.AppHttpClient
import com.blockstream.network.NetworkResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import lwk.Script
import lwk.TxOut

class WalletAbiEsploraHttpClient(appInfo: AppInfo) :
    AppHttpClient(enableLogging = appInfo.isDevelopmentOrDebug) {
    suspend fun getTransactionHex(
        apiBaseUrl: String,
        txid: String,
    ): NetworkResponse<String> {
        val normalizedBaseUrl = apiBaseUrl.trim().trimEnd('/')
        return get("$normalizedBaseUrl/tx/$txid/hex")
    }

    suspend fun getTransactionOutspends(
        apiBaseUrl: String,
        txid: String,
    ): NetworkResponse<List<WalletAbiEsploraOutspend>> {
        val normalizedBaseUrl = apiBaseUrl.trim().trimEnd('/')
        return get("$normalizedBaseUrl/tx/$txid/outspends")
    }
}

@Serializable
data class WalletAbiEsploraVout(
    @SerialName("scriptpubkey")
    val scriptPubkey: String? = null,
    @SerialName("scriptpubkey_hex")
    val scriptPubkeyHex: String? = null,
    val value: Long? = null,
    val asset: String? = null,
)

@Serializable
data class WalletAbiEsploraOutspend(
    val spent: Boolean = false,
)

fun String?.toWalletAbiEsploraApiBaseUrl(): String? {
    val trimmed = this?.trim()?.trimEnd('/') ?: return null
    if (trimmed.isBlank()) {
        return null
    }
    if (trimmed.endsWith("/api")) {
        return trimmed
    }

    val withoutTx = trimmed.removeSuffix("/tx")
    if (withoutTx != trimmed) {
        return if (withoutTx.endsWith("/api")) {
            withoutTx
        } else {
            "$withoutTx/api"
        }
    }

    return "$trimmed/api"
}

fun GdkNetwork.walletAbiEsploraApiBaseUrls(): List<String> {
    return buildList {
        explorerUrl.toWalletAbiEsploraApiBaseUrl()?.let(::add)
        addAll(walletAbiDefaultEsploraApiBaseUrls())
    }.distinct()
}

private fun GdkNetwork.walletAbiDefaultEsploraApiBaseUrls(): List<String> {
    return when (canonicalNetworkId) {
        GdkNetwork.GreenMainnet -> listOf("https://blockstream.info/api")
        GdkNetwork.GreenTestnet -> listOf("https://blockstream.info/testnet/api")
        GdkNetwork.GreenLiquid -> listOf(
            "https://blockstream.info/liquid/api",
            "https://liquid.network/liquid/api",
        )

        GdkNetwork.GreenTestnetLiquid -> listOf(
            "https://blockstream.info/liquidtestnet/api",
            "https://liquid.network/liquidtestnet/api",
        )

        else -> emptyList()
    }
}

fun WalletAbiEsploraVout.toExplicitTxOutOrNull(): TxOut? {
    val scriptHex = (scriptPubkey ?: scriptPubkeyHex).orEmpty().trim()
    val assetId = asset.orEmpty().trim()
    val amount = value?.takeIf { it >= 0L }?.toULong() ?: return null
    if (scriptHex.isEmpty() || assetId.isEmpty()) {
        return null
    }

    val script = runCatching { Script(scriptHex) }.getOrNull() ?: return null
    return TxOut.fromExplicit(script, assetId, amount)
}
