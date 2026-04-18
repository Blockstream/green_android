package com.blockstream.domain.walletabi.request

import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DefaultWalletAbiRequestParserTest {
    private val parser = DefaultWalletAbiRequestParser()

    @Test
    fun parse_accepts_valid_process_request_envelope() {
        val result = parser.parse(validEnvelopeJson())

        val success = assertIs<WalletAbiRequestParseResult.Success>(result)
        assertEquals(JsonPrimitive("demo-id"), success.envelope.id)
        assertEquals(WalletAbiMethod.PROCESS_REQUEST, success.envelope.method)

        val request = assertIs<WalletAbiParsedRequest.TxCreate>(success.envelope.request).request
        assertEquals("wallet-abi-0.1", request.abiVersion)
        assertEquals("request-123", request.requestId)
        assertEquals(WalletAbiNetwork.TESTNET_LIQUID, request.network)
        assertEquals(true, request.broadcast)
        assertEquals(1, request.params.inputs.size)
        assertEquals(1, request.params.outputs.size)
        assertEquals(12.5f, request.params.feeRateSatKvb)
        assertEquals(JsonPrimitive(500000), request.params.lockTime)
    }

    @Test
    fun parse_rejects_malformed_envelope_json() {
        assertEquals(
            WalletAbiRequestValidationError.MalformedEnvelopeJson,
            parseFailure("{")
        )
    }

    @Test
    fun parse_rejects_missing_method() {
        assertEquals(
            WalletAbiRequestValidationError.MissingMethod,
            parseFailure(validEnvelopeJson(methodField = ""))
        )
    }

    @Test
    fun parse_rejects_blank_method() {
        assertEquals(
            WalletAbiRequestValidationError.MissingMethod,
            parseFailure(validEnvelopeJson(method = "   "))
        )
    }

    @Test
    fun parse_rejects_unsupported_method() {
        assertEquals(
            WalletAbiRequestValidationError.UnsupportedMethod("wallet_abi_get_account"),
            parseFailure(validEnvelopeJson(method = "wallet_abi_get_account"))
        )
    }

    @Test
    fun parse_rejects_missing_params() {
        assertEquals(
            WalletAbiRequestValidationError.MissingParams,
            parseFailure(validEnvelopeJson(paramsField = ""))
        )
    }

    @Test
    fun parse_rejects_unsupported_abi_version() {
        assertEquals(
            WalletAbiRequestValidationError.UnsupportedAbiVersion("wallet-abi-0.2"),
            parseFailure(validEnvelopeJson(params = validParamsJson(abiVersion = "wallet-abi-0.2")))
        )
    }

    @Test
    fun parse_rejects_noncanonical_network() {
        assertEquals(
            WalletAbiRequestValidationError.UnsupportedNetwork("Liquid"),
            parseFailure(validEnvelopeJson(params = validParamsJson(network = "Liquid")))
        )
    }

    @Test
    fun parse_rejects_blank_request_id() {
        assertEquals(
            WalletAbiRequestValidationError.BlankRequestId,
            parseFailure(validEnvelopeJson(params = validParamsJson(requestId = "  ")))
        )
    }

    @Test
    fun parse_rejects_duplicate_input_ids() {
        assertEquals(
            WalletAbiRequestValidationError.DuplicateInputId("input-1"),
            parseFailure(
                validEnvelopeJson(
                    params = validParamsJson(
                        inputs = """
                            [
                              {
                                "id": "input-1",
                                "utxo_source": { "kind": "wallet" },
                                "unblinding": { "kind": "known" },
                                "sequence": 1,
                                "finalizer": { "kind": "default" }
                              },
                              {
                                "id": "input-1",
                                "utxo_source": { "kind": "wallet" },
                                "unblinding": { "kind": "known" },
                                "sequence": 2,
                                "finalizer": { "kind": "default" }
                              }
                            ]
                        """.trimIndent()
                    )
                )
            )
        )
    }

    @Test
    fun parse_rejects_duplicate_output_ids() {
        assertEquals(
            WalletAbiRequestValidationError.DuplicateOutputId("output-1"),
            parseFailure(
                validEnvelopeJson(
                    params = validParamsJson(
                        outputs = """
                            [
                              {
                                "id": "output-1",
                                "amount_sat": 1000,
                                "lock": { "kind": "pkh" },
                                "asset": { "kind": "btc" },
                                "blinder": { "kind": "default" }
                              },
                              {
                                "id": "output-1",
                                "amount_sat": 2000,
                                "lock": { "kind": "pkh" },
                                "asset": { "kind": "btc" },
                                "blinder": { "kind": "default" }
                              }
                            ]
                        """.trimIndent()
                    )
                )
            )
        )
    }

    @Test
    fun parse_rejects_non_positive_output_amount() {
        assertEquals(
            WalletAbiRequestValidationError.NonPositiveOutputAmount("output-1", 0L),
            parseFailure(validEnvelopeJson(params = validParamsJson(amountSat = 0L)))
        )
    }

    @Test
    fun parse_rejects_invalid_fee_rate() {
        assertEquals(
            WalletAbiRequestValidationError.InvalidFeeRate(0f),
            parseFailure(validEnvelopeJson(params = validParamsJson(feeRateSatKvb = 0f)))
        )
    }

    @Test
    fun parse_tolerates_unknown_extra_keys() {
        val result = parser.parse(
            validEnvelopeJson(
                params = validParamsJson(
                    extraParamsFields = """
                        ,
                        "unknown_top_level": "accepted",
                        "inputs": [
                          {
                            "id": "input-1",
                            "utxo_source": { "kind": "wallet" },
                            "unblinding": { "kind": "known" },
                            "sequence": 1,
                            "finalizer": { "kind": "default" },
                            "unknown_input_field": "accepted"
                          }
                        ]
                    """.trimIndent()
                ),
                extraEnvelopeFields = """
                    ,
                    "unknown_envelope_field": "accepted"
                """.trimIndent()
            )
        )

        assertIs<WalletAbiRequestParseResult.Success>(result)
    }

    private fun parseFailure(json: String): WalletAbiRequestValidationError {
        return assertIs<WalletAbiRequestParseResult.Failure>(parser.parse(json)).error
    }

    private fun validEnvelopeJson(
        method: String = WalletAbiMethod.PROCESS_REQUEST.wireValue,
        params: String = validParamsJson(),
        methodField: String = "\"method\": \"$method\",",
        paramsField: String = "\"params\": $params,",
        extraEnvelopeFields: String = ""
    ): String {
        return """
            {
              "jsonrpc": "2.0",
              "id": "demo-id",
              $methodField
              $paramsField
              "ignored": "value"
              $extraEnvelopeFields
            }
        """.trimIndent()
    }

    private fun validParamsJson(
        abiVersion: String = "wallet-abi-0.1",
        requestId: String = "request-123",
        network: String = "testnet-liquid",
        amountSat: Long = 1_000L,
        inputs: String = """
            [
              {
                "id": "input-1",
                "utxo_source": { "kind": "wallet" },
                "unblinding": { "kind": "known" },
                "sequence": 1,
                "finalizer": { "kind": "default" }
              }
            ]
        """.trimIndent(),
        outputs: String = """
            [
              {
                "id": "output-1",
                "amount_sat": $amountSat,
                "lock": { "kind": "pkh" },
                "asset": { "kind": "btc" },
                "blinder": { "kind": "default" }
              }
            ]
        """.trimIndent(),
        feeRateSatKvb: Float = 12.5f,
        extraParamsFields: String = ""
    ): String {
        return """
            {
              "abi_version": "$abiVersion",
              "request_id": "$requestId",
              "network": "$network",
              "params": {
                "inputs": $inputs,
                "outputs": $outputs,
                "fee_rate_sat_kvb": $feeRateSatKvb,
                "lock_time": 500000
              },
              "broadcast": true
              $extraParamsFields
            }
        """.trimIndent()
    }
}
