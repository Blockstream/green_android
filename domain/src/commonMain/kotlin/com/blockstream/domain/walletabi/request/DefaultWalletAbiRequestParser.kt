package com.blockstream.domain.walletabi.request

import com.blockstream.data.json.DefaultJson
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class DefaultWalletAbiRequestParser : WalletAbiRequestParser {
    override fun parse(requestEnvelopeJson: String): WalletAbiRequestParseResult {
        val envelope = try {
            DefaultJson.parseToJsonElement(requestEnvelopeJson).jsonObject
        } catch (_: Exception) {
            return WalletAbiRequestParseResult.Failure(
                WalletAbiRequestValidationError.MalformedEnvelopeJson
            )
        }

        val method = try {
            envelope["method"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        } ?: return WalletAbiRequestParseResult.Failure(
            WalletAbiRequestValidationError.MissingMethod
        )

        return when (method) {
            WalletAbiMethod.PROCESS_REQUEST.wireValue -> parseProcessRequest(
                id = envelope["id"],
                params = envelope["params"]
            )

            else -> WalletAbiRequestParseResult.Failure(
                WalletAbiRequestValidationError.UnsupportedMethod(method)
            )
        }
    }

    private fun parseProcessRequest(
        id: JsonElement?,
        params: JsonElement?
    ): WalletAbiRequestParseResult {
        val paramsElement = params?.takeUnless { it is JsonNull }
            ?: return WalletAbiRequestParseResult.Failure(
                WalletAbiRequestValidationError.MissingParams
            )

        val requestObject = paramsElement.asObject()
            ?: return WalletAbiRequestParseResult.Failure(
                WalletAbiRequestValidationError.MalformedRequestParams
            )
        val abiVersion = requestObject.requiredString("abi_version")
            ?: return WalletAbiRequestParseResult.Failure(
                WalletAbiRequestValidationError.MalformedRequestParams
            )
        if (abiVersion != SUPPORTED_ABI_VERSION) {
            return WalletAbiRequestParseResult.Failure(
                WalletAbiRequestValidationError.UnsupportedAbiVersion(abiVersion)
            )
        }

        val networkName = requestObject.requiredString("network")
            ?: return WalletAbiRequestParseResult.Failure(
                WalletAbiRequestValidationError.MalformedRequestParams
            )
        val network = WalletAbiNetwork.entries.firstOrNull { it.wireValue == networkName }
            ?: return WalletAbiRequestParseResult.Failure(
                WalletAbiRequestValidationError.UnsupportedNetwork(networkName)
            )

        val requestId = requestObject.requiredString("request_id")
            ?: return WalletAbiRequestParseResult.Failure(
                WalletAbiRequestValidationError.MalformedRequestParams
            )
        if (requestId.isBlank()) {
            return WalletAbiRequestParseResult.Failure(
                WalletAbiRequestValidationError.BlankRequestId
            )
        }

        val runtimeParams = requestObject["params"]?.asObject()
            ?: return WalletAbiRequestParseResult.Failure(
                WalletAbiRequestValidationError.MalformedRequestParams
            )
        val inputs = runtimeParams["inputs"]?.asArray()
            ?: return WalletAbiRequestParseResult.Failure(
                WalletAbiRequestValidationError.MalformedRequestParams
            )
        val outputs = runtimeParams["outputs"]?.asArray()
            ?: return WalletAbiRequestParseResult.Failure(
                WalletAbiRequestValidationError.MalformedRequestParams
            )
        val feeRateSatKvb = runtimeParams.optionalFloat("fee_rate_sat_kvb")
        if ("fee_rate_sat_kvb" in runtimeParams && feeRateSatKvb == null) {
            return WalletAbiRequestParseResult.Failure(
                WalletAbiRequestValidationError.MalformedRequestParams
            )
        }
        val broadcast = requestObject.requiredBoolean("broadcast")
            ?: return WalletAbiRequestParseResult.Failure(
                WalletAbiRequestValidationError.MalformedRequestParams
            )

        val inputIds = linkedSetOf<String>()
        val parsedInputs = inputs.mapIndexed { index, inputElement ->
            val input = inputElement.asObject()
                ?: return WalletAbiRequestParseResult.Failure(
                    WalletAbiRequestValidationError.MalformedRequestParams
                )
            val normalizedId = input.requiredString("id")?.trim()
                ?: return WalletAbiRequestParseResult.Failure(
                    WalletAbiRequestValidationError.MalformedRequestParams
                )
            when {
                normalizedId.isEmpty() -> return WalletAbiRequestParseResult.Failure(
                    WalletAbiRequestValidationError.BlankInputId(index)
                )

                !inputIds.add(normalizedId) -> {
                    return WalletAbiRequestParseResult.Failure(
                        WalletAbiRequestValidationError.DuplicateInputId(normalizedId)
                    )
                }

                else -> WalletAbiInput(
                    id = normalizedId,
                    utxoSource = input["utxo_source"]
                        ?: return WalletAbiRequestParseResult.Failure(
                            WalletAbiRequestValidationError.MalformedRequestParams
                        ),
                    unblinding = input["unblinding"]
                        ?: return WalletAbiRequestParseResult.Failure(
                            WalletAbiRequestValidationError.MalformedRequestParams
                        ),
                    sequence = input.requiredLong("sequence")
                        ?: return WalletAbiRequestParseResult.Failure(
                            WalletAbiRequestValidationError.MalformedRequestParams
                        ),
                    issuance = input["issuance"],
                    finalizer = input["finalizer"]
                        ?: return WalletAbiRequestParseResult.Failure(
                            WalletAbiRequestValidationError.MalformedRequestParams
                        )
                )
            }
        }

        val outputIds = linkedSetOf<String>()
        val parsedOutputs = outputs.mapIndexed { index, outputElement ->
            val output = outputElement.asObject()
                ?: return WalletAbiRequestParseResult.Failure(
                    WalletAbiRequestValidationError.MalformedRequestParams
                )
            val normalizedId = output.requiredString("id")?.trim()
                ?: return WalletAbiRequestParseResult.Failure(
                    WalletAbiRequestValidationError.MalformedRequestParams
                )
            val amountSat = output.requiredLong("amount_sat")
                ?: return WalletAbiRequestParseResult.Failure(
                    WalletAbiRequestValidationError.MalformedRequestParams
                )
            when {
                normalizedId.isEmpty() -> return WalletAbiRequestParseResult.Failure(
                    WalletAbiRequestValidationError.BlankOutputId(index)
                )

                !outputIds.add(normalizedId) -> {
                    return WalletAbiRequestParseResult.Failure(
                        WalletAbiRequestValidationError.DuplicateOutputId(normalizedId)
                    )
                }

                amountSat <= 0L -> return WalletAbiRequestParseResult.Failure(
                    WalletAbiRequestValidationError.NonPositiveOutputAmount(
                        id = normalizedId,
                        amountSat = amountSat
                    )
                )

                else -> WalletAbiOutput(
                    id = normalizedId,
                    amountSat = amountSat,
                    lock = output["lock"]
                        ?: return WalletAbiRequestParseResult.Failure(
                            WalletAbiRequestValidationError.MalformedRequestParams
                        ),
                    asset = output["asset"]
                        ?: return WalletAbiRequestParseResult.Failure(
                            WalletAbiRequestValidationError.MalformedRequestParams
                        ),
                    blinder = output["blinder"]
                        ?: return WalletAbiRequestParseResult.Failure(
                            WalletAbiRequestValidationError.MalformedRequestParams
                        )
                )
            }
        }

        feeRateSatKvb?.let { feeRate ->
            if (feeRate <= 0f) {
                return WalletAbiRequestParseResult.Failure(
                    WalletAbiRequestValidationError.InvalidFeeRate(feeRate)
                )
            }
        }

        return WalletAbiRequestParseResult.Success(
            WalletAbiParsedEnvelope(
                id = id,
                method = WalletAbiMethod.PROCESS_REQUEST,
                request = WalletAbiParsedRequest.TxCreate(
                    request = WalletAbiTxCreateRequest(
                        abiVersion = abiVersion,
                        requestId = requestId,
                        network = network,
                        params = WalletAbiRuntimeParams(
                            inputs = parsedInputs,
                            outputs = parsedOutputs,
                            feeRateSatKvb = feeRateSatKvb,
                            lockTime = runtimeParams["lock_time"]
                        ),
                        broadcast = broadcast
                    )
                )
            )
        )
    }

    private companion object {
        const val SUPPORTED_ABI_VERSION = "wallet-abi-0.1"
    }
}

private fun JsonElement.asObject(): JsonObject? = runCatching { jsonObject }.getOrNull()

private fun JsonElement.asArray() = runCatching { jsonArray }.getOrNull()

private fun JsonObject.requiredString(key: String): String? {
    return this[key]?.let { element ->
        runCatching { element.jsonPrimitive.contentOrNull }.getOrNull()
    }
}

private fun JsonObject.requiredLong(key: String): Long? {
    return requiredString(key)?.toLongOrNull()
}

private fun JsonObject.requiredBoolean(key: String): Boolean? {
    return this[key]?.let { element ->
        runCatching { element.jsonPrimitive.booleanOrNull }.getOrNull()
    }
}

private fun JsonObject.optionalFloat(key: String): Float? {
    return this[key]?.let { element ->
        runCatching { element.jsonPrimitive.contentOrNull?.toFloatOrNull() }.getOrNull()
    }
}
