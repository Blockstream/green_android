package com.blockstream.libwally

typealias WallyWords = Any

class KotlinWally {

    val BIP39_WORDLIST_LEN
        get() = Wally.BIP39_WORDLIST_LEN

    fun init(flags: Long, randomBytes: ByteArray) {
        Wally.init(flags)
        Wally.secp_randomize(randomBytes)
    }

    fun bip39Wordlist(lang: String): WallyWords = Wally.bip39_get_wordlist(lang)

    fun bip39Word(words: WallyWords, i: Int) = Wally.bip39_get_word(words, i.toLong())

    fun bip39MnemonicValidate(wordList: WallyWords, mnemonic: String) : Boolean {
        return try {
            Wally.bip39_mnemonic_validate(wordList, mnemonic)
            true
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            false
        }
    }

    fun bip32KeyFromBase58(base58: String) = Wally.bip32_key_from_base58(base58)

    fun bip32Fingerprint(bip32xPub: String): String {

        Wally.bip32_key_from_base58(bip32xPub).also { ext_key ->
            try{
                return Wally.hex_from_bytes(Wally.bip32_key_get_fingerprint(ext_key))
            }finally {
                Wally.bip32_key_free(ext_key)
            }
        }

        null
    }


    companion object{
        const val WALLY_SECP_RANDOMIZE_LEN = Wally.WALLY_SECP_RANDOMIZE_LEN
    }

}