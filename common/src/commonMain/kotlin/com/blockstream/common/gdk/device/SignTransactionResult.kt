package com.blockstream.common.gdk.device

data class SignTransactionResult(val signatures: List<String>, val signerCommitments: List<String>?)