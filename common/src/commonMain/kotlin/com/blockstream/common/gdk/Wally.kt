package com.blockstream.common.gdk

interface Wally {
    val aesBlockLen: Int
    val hmacSha256Len: Int
    val ecPrivateKeyLen: Int
    val ecSignatureRecoverableLen: Int
    val bip39TotalWords: Int
    val blindingFactorLen: Int

    fun ecPrivateKeyVerify(privateKey: ByteArray): Boolean
    fun ecSigToDer(signature: ByteArray): String

    fun bip39GetWord(index: Int): String
    fun bip39MnemonicValidate(mnemonic: String): Boolean

    fun isXpubValid(xpub: String): Boolean
    fun bip32Fingerprint(bip32xPub: String): String?

    fun hashPrevouts(
        txHashes: ByteArray,
        utxoIndexes: List<Int>
    ): ByteArray

    fun recoveryXpubBranchDerivation(
        recoveryXpub: String,
        branch: Long
    ): String

    fun bip85FromMnemonic(
        mnemonic: String,
        passphrase: String?,
        isTestnet: Boolean = false,
        index: Long = 0,
        numOfWords: Long = 12
    ): String?

    fun bip85FromJade(privateKey: ByteArray, publicKey: ByteArray, label: String, payload: ByteArray): String?

    companion object {
        const val BIP39_WORD_LIST_LANG = "en"
    }
}

private var _bip39WordList: List<String>? = null
fun Wally.getBip39WordList(): List<String> {
    return _bip39WordList ?: run {
        val wordList = mutableListOf<String>()
        for (i in 0 until this.bip39TotalWords) {
            wordList += this.bip39GetWord(i)
        }

        _bip39WordList = wordList

        wordList
    }
}

expect fun getWally(): Wally