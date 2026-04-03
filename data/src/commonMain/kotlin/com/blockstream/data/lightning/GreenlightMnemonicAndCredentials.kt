package com.blockstream.data.lightning

data class GreenlightMnemonicAndCredentials constructor(
    val mnemonic: String,
    val credentials: ByteArray?
)