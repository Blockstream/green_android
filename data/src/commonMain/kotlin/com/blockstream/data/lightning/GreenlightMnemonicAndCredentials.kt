package com.blockstream.data.lightning

data class GreenlightMnemonicAndCredentials(
    val mnemonic: String,
    val credentials: ByteArray?
)