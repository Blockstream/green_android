package com.blockstream.domain.walletabi.request

interface WalletAbiRequestParser {
    fun parse(requestEnvelopeJson: String): WalletAbiRequestParseResult
}

sealed interface WalletAbiRequestParseResult {
    data class Success(
        val envelope: WalletAbiParsedEnvelope
    ) : WalletAbiRequestParseResult

    data class Failure(
        val error: WalletAbiRequestValidationError
    ) : WalletAbiRequestParseResult
}

sealed interface WalletAbiRequestValidationError {
    val message: String

    data object MalformedEnvelopeJson : WalletAbiRequestValidationError {
        override val message: String = "Wallet ABI request envelope is malformed"
    }

    data object MissingMethod : WalletAbiRequestValidationError {
        override val message: String = "Wallet ABI request method is missing"
    }

    data class UnsupportedMethod(
        val method: String?
    ) : WalletAbiRequestValidationError {
        override val message: String =
            method?.takeIf { it.isNotBlank() }?.let { "Unsupported Wallet ABI method '$it'" }
                ?: "Wallet ABI method is unsupported"
    }

    data object MissingParams : WalletAbiRequestValidationError {
        override val message: String = "Wallet ABI request params are missing"
    }

    data object MalformedRequestParams : WalletAbiRequestValidationError {
        override val message: String = "Wallet ABI request params are malformed"
    }

    data class UnsupportedAbiVersion(
        val abiVersion: String?
    ) : WalletAbiRequestValidationError {
        override val message: String =
            abiVersion?.takeIf { it.isNotBlank() }?.let { "Unsupported Wallet ABI version '$it'" }
                ?: "Wallet ABI version is unsupported"
    }

    data class UnsupportedNetwork(
        val network: String?
    ) : WalletAbiRequestValidationError {
        override val message: String =
            network?.takeIf { it.isNotBlank() }?.let { "Unsupported Wallet ABI network '$it'" }
                ?: "Wallet ABI network is unsupported"
    }

    data object BlankRequestId : WalletAbiRequestValidationError {
        override val message: String = "Wallet ABI request id must not be blank"
    }

    data class BlankInputId(
        val index: Int
    ) : WalletAbiRequestValidationError {
        override val message: String = "Wallet ABI input id at index $index must not be blank"
    }

    data class DuplicateInputId(
        val id: String
    ) : WalletAbiRequestValidationError {
        override val message: String = "Wallet ABI input id '$id' is duplicated"
    }

    data class BlankOutputId(
        val index: Int
    ) : WalletAbiRequestValidationError {
        override val message: String = "Wallet ABI output id at index $index must not be blank"
    }

    data class DuplicateOutputId(
        val id: String
    ) : WalletAbiRequestValidationError {
        override val message: String = "Wallet ABI output id '$id' is duplicated"
    }

    data class NonPositiveOutputAmount(
        val id: String,
        val amountSat: Long
    ) : WalletAbiRequestValidationError {
        override val message: String =
            "Wallet ABI output '$id' must have a positive amount, got $amountSat"
    }

    data class InvalidFeeRate(
        val feeRateSatKvb: Float
    ) : WalletAbiRequestValidationError {
        override val message: String =
            "Wallet ABI fee rate must be positive, got $feeRateSatKvb"
    }
}
