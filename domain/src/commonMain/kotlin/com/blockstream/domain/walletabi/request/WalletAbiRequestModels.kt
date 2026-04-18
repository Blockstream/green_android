package com.blockstream.domain.walletabi.request

import kotlinx.serialization.json.JsonElement

data class WalletAbiParsedEnvelope(
    val id: JsonElement?,
    val method: WalletAbiMethod,
    val request: WalletAbiParsedRequest
)

enum class WalletAbiMethod(
    val wireValue: String
) {
    PROCESS_REQUEST("wallet_abi_process_request")
}

sealed interface WalletAbiParsedRequest {
    data class TxCreate(
        val request: WalletAbiTxCreateRequest
    ) : WalletAbiParsedRequest
}

enum class WalletAbiNetwork(
    val wireValue: String
) {
    LIQUID("liquid"),
    TESTNET_LIQUID("testnet-liquid"),
    LOCALTEST_LIQUID("localtest-liquid")
}

data class WalletAbiTxCreateRequest(
    val abiVersion: String,
    val requestId: String,
    val network: WalletAbiNetwork,
    val params: WalletAbiRuntimeParams,
    val broadcast: Boolean
)

data class WalletAbiRuntimeParams(
    val inputs: List<WalletAbiInput>,
    val outputs: List<WalletAbiOutput>,
    val feeRateSatKvb: Float? = null,
    val lockTime: JsonElement? = null
)

data class WalletAbiInput(
    val id: String,
    val utxoSource: JsonElement,
    val unblinding: JsonElement,
    val sequence: Long,
    val issuance: JsonElement? = null,
    val finalizer: JsonElement
)

data class WalletAbiOutput(
    val id: String,
    val amountSat: Long,
    val lock: JsonElement,
    val asset: JsonElement,
    val blinder: JsonElement
)
