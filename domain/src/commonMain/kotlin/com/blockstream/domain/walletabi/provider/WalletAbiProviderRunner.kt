package com.blockstream.domain.walletabi.provider

import com.blockstream.data.walletabi.provider.WalletAbiEsploraHttpClient
import com.blockstream.data.walletabi.provider.WalletAbiWalletSnapshotSupport
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.json.DefaultJson
import com.blockstream.domain.walletabi.request.WalletAbiTxCreateRequest
import com.blockstream.domain.walletabi.request.WalletAbiNetwork
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import lwk.Mnemonic
import lwk.Signer
import lwk.SignerMetaLink
import lwk.WalletAbiProvider
import lwk.WalletAbiSignerContext
import lwk.WalletBroadcasterLink
import lwk.WalletOutputAllocatorLink
import lwk.WalletPrevoutResolverLink
import lwk.WalletReceiveAddressProviderLink
import lwk.WalletRuntimeDepsLink
import lwk.WalletSessionFactoryLink
import lwk.Network as LwkNetwork

private const val WALLET_ABI_PROCESS_REQUEST_METHOD = "wallet_abi_process_request"

data class WalletAbiProviderRunResult(
    val response: WalletAbiProviderProcessResponse,
    val responseJson: String,
)

interface WalletAbiProviderRunning {
    suspend fun run(
        context: WalletAbiExecutionContext,
        request: WalletAbiTxCreateRequest,
    ): WalletAbiProviderRunResult
}

class WalletAbiProviderRunner(
    private val json: Json = DefaultJson,
    private val esploraHttpClient: WalletAbiEsploraHttpClient,
) : WalletAbiProviderRunning {
    override suspend fun run(
        context: WalletAbiExecutionContext,
        request: WalletAbiTxCreateRequest,
    ): WalletAbiProviderRunResult {
        if (request.network != context.requestNetwork) {
            throw WalletAbiExecutionContextException(
                "Wallet ABI request network mismatch: request=${request.network.wireValue} context=${context.requestNetwork.wireValue}",
            )
        }

        return withProvider(context) { provider ->
            val requestJson = json.encodeToString(request)
            val providerRequestJson = normalizeWalletAbiProviderRequestJson(
                json = json,
                requestJson = requestJson,
            )
            val providerResponseJson = provider.dispatchJson(
                method = WALLET_ABI_PROCESS_REQUEST_METHOD,
                paramsJson = providerRequestJson,
            )
            val response = json.decodeFromString(
                WalletAbiProviderProcessResponse.serializer(),
                normalizeWalletAbiProviderResponseJson(
                    json = json,
                    responseJson = providerResponseJson,
                ),
            )
            WalletAbiProviderRunResult(
                response = response,
                responseJson = providerResponseJson,
            )
        }
    }

    private suspend fun <T> withProvider(
        context: WalletAbiExecutionContext,
        block: (WalletAbiProvider) -> T,
    ): T {
        var mnemonic: Mnemonic? = null
        var signer: Signer? = null
        var signerLink: SignerMetaLink? = null
        var sessionFactoryLink: WalletSessionFactoryLink? = null
        var outputAllocatorLink: WalletOutputAllocatorLink? = null
        var prevoutResolverLink: WalletPrevoutResolverLink? = null
        var broadcasterLink: WalletBroadcasterLink? = null
        var receiveAddressProviderLink: WalletReceiveAddressProviderLink? = null
        var walletRuntimeDepsLink: WalletRuntimeDepsLink? = null
        var provider: WalletAbiProvider? = null

        try {
            val mnemonicString = context.session.getCredentials().mnemonic
                ?.takeIf { it.isNotBlank() }
                ?: throw WalletAbiExecutionContextException(
                    "Wallet ABI software signer requires mnemonic credentials",
                )
            val lwkNetwork = context.requestNetwork.toLwkNetwork()

            val currentMnemonic = Mnemonic(mnemonicString)
            mnemonic = currentMnemonic

            val currentSigner = Signer(currentMnemonic, lwkNetwork)
            signer = currentSigner

            signerLink = SignerMetaLink.fromSoftwareSigner(
                signer = currentSigner,
                context = WalletAbiSignerContext(
                    network = lwkNetwork,
                    accountIndex = context.primaryAccount.walletAbiAccountIndex(),
                ),
            )

            val snapshotSupport = WalletAbiWalletSnapshotSupport(
                session = context.session,
                primaryAccount = context.primaryAccount,
                snapshotAccounts = context.accounts,
                lwkNetwork = lwkNetwork,
                esploraHttpClient = esploraHttpClient,
            )

            sessionFactoryLink = WalletSessionFactoryLink(snapshotSupport)
            outputAllocatorLink = WalletOutputAllocatorLink(snapshotSupport)
            prevoutResolverLink = WalletPrevoutResolverLink(snapshotSupport)
            broadcasterLink = WalletBroadcasterLink(snapshotSupport)
            receiveAddressProviderLink = WalletReceiveAddressProviderLink(snapshotSupport)

            walletRuntimeDepsLink = WalletRuntimeDepsLink(
                sessionFactory = sessionFactoryLink,
                outputAllocator = outputAllocatorLink,
                prevoutResolver = prevoutResolverLink,
                broadcaster = broadcasterLink,
                receiveAddressProvider = receiveAddressProviderLink,
            )

            provider = WalletAbiProvider(
                signer = signerLink,
                wallet = walletRuntimeDepsLink,
            )

            return block(provider)
        } finally {
            provider?.close()
            walletRuntimeDepsLink?.close()
            receiveAddressProviderLink?.close()
            broadcasterLink?.close()
            prevoutResolverLink?.close()
            outputAllocatorLink?.close()
            sessionFactoryLink?.close()
            signerLink?.close()
            signer?.close()
            mnemonic?.close()
        }
    }
}

private fun WalletAbiNetwork.toLwkNetwork(): LwkNetwork {
    return when (this) {
        WalletAbiNetwork.LIQUID -> LwkNetwork.mainnet()
        WalletAbiNetwork.TESTNET_LIQUID -> LwkNetwork.testnet()
        WalletAbiNetwork.LOCALTEST_LIQUID -> LwkNetwork.regtestDefault()
    }
}

internal fun normalizeWalletAbiProviderRequestJson(
    json: Json,
    requestJson: String,
): String {
    val payload = runCatching {
        json.parseToJsonElement(requestJson).jsonObject
    }.getOrNull() ?: return requestJson

    val normalizedNetwork = when (payload["network"]?.jsonPrimitive?.contentOrNull) {
        "liquid" -> JsonPrimitive("Liquid")
        "testnet-liquid" -> JsonPrimitive("LiquidTestnet")
        else -> return requestJson
    }

    return rewriteWalletAbiProviderNetwork(payload, normalizedNetwork)
}

internal fun normalizeWalletAbiProviderResponseJson(
    json: Json,
    responseJson: String,
): String {
    val payload = runCatching {
        json.parseToJsonElement(responseJson).jsonObject
    }.getOrNull() ?: return responseJson

    val normalizedNetwork = when (val network = payload["network"]) {
        is JsonPrimitive -> when (network.contentOrNull) {
            "Liquid" -> JsonPrimitive("liquid")
            "LiquidTestnet" -> JsonPrimitive("testnet-liquid")
            else -> null
        }

        is JsonObject -> {
            if ("ElementsRegtest" in network) {
                JsonPrimitive("localtest-liquid")
            } else {
                null
            }
        }

        else -> null
    } ?: return responseJson

    return rewriteWalletAbiProviderNetwork(payload, normalizedNetwork)
}

private fun rewriteWalletAbiProviderNetwork(
    payload: JsonObject,
    network: JsonPrimitive,
): String {
    return buildString {
        append('{')
        payload.entries.forEachIndexed { index, (key, value) ->
            if (index > 0) {
                append(',')
            }
            append(JsonPrimitive(key))
            append(':')
            append(if (key == "network") network else value)
        }
        append('}')
    }
}
