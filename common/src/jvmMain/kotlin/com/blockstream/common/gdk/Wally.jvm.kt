package com.blockstream.common.gdk

actual fun getWally(): Wally {
    return object : Wally{
        override val aesBlockLen: Int
            get() = TODO("Not yet implemented")
        override val hmacSha256Len: Int
            get() = TODO("Not yet implemented")
        override val ecPrivateKeyLen: Int
            get() = TODO("Not yet implemented")
        override val bip39TotalWords: Int
            get() = TODO("Not yet implemented")
        override val blindingFactorLen: Int
            get() = TODO("Not yet implemented")
        override val ecSignatureRecoverableLen: Int
            get() = TODO("Not yet implemented")

        override fun recoveryChainCodeBranchDerivation(
            version: Int,
            depth: Int,
            childNum: Int,
            chainCode: ByteArray,
            publicKey: ByteArray,
            branch: Long
        ): String {
            TODO("Not yet implemented")
        }

        override fun ecPrivateKeyVerify(privateKey: ByteArray): Boolean {
            TODO("Not yet implemented")
        }

        override fun ecSigToDer(signature: ByteArray): String {
            TODO("Not yet implemented")
        }

        override fun bip39GetWord(index: Int): String {
            TODO("Not yet implemented")
        }

        override fun bip39MnemonicValidate(mnemonic: String): Boolean {
            TODO("Not yet implemented")
        }

        override fun isXpubValid(xpub: String): Boolean {
            TODO("Not yet implemented")
        }

        override fun bip32Fingerprint(bip32xPub: String): String? {
            TODO("Not yet implemented")
        }

        override fun hashPrevouts(txHashes: ByteArray, utxoIndexes: List<Int>): ByteArray {
            TODO("Not yet implemented")
        }

        override fun bip85FromMnemonic(
            mnemonic: String,
            passphrase: String?,
            isTestnet: Boolean,
            index: Long,
            numOfWords: Long
        ): String? {
            TODO("Not yet implemented")
        }

        override fun bip85FromJade(
            privateKey: ByteArray,
            publicKey: ByteArray,
            label: String,
            payload: ByteArray
        ): String? {
            TODO("Not yet implemented")
        }

    }
}