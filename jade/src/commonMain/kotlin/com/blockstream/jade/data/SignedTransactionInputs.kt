package com.blockstream.jade.data

@OptIn(ExperimentalStdlibApi::class)
class SignedTransactionInputs(
    val signatures: List<String>,
    val signerCommitments: List<String>? = null
) {
    companion object {
        fun create(
            signatures: List<ByteArray>,
            signerCommitments: List<ByteArray>
        ) = SignedTransactionInputs(
            signatures = signatures.map { it.toHexString() },
            signerCommitments = signerCommitments.map { it.toHexString() }
        )
    }
}
