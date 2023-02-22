package com.blockstream.libwally

import com.blockstream.libwally.Wally.BIP32_VER_MAIN_PRIVATE
import com.blockstream.libwally.Wally.BIP32_VER_TEST_PRIVATE

class KotlinWally {

     private val bip39WordList by lazy { Wally.bip39_get_wordlist(BIP39_WORD_LIST_LANG) }

    val BIP39_WORDLIST_LEN
        get() = Wally.BIP39_WORDLIST_LEN.toLong()

    fun init(flags: Long, randomBytes: ByteArray) {
        Wally.init(flags)
        Wally.secp_randomize(randomBytes)
    }

    fun bip39Word(index: Long) = Wally.bip39_get_word(bip39WordList, index)

    fun bip39MnemonicValidate(mnemonic: String) : Boolean {
        return try {
            Wally.bip39_mnemonic_validate(bip39WordList, mnemonic)
            true
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            false
        }
    }

    fun bip32KeyFromBase58(base58: String) = Wally.bip32_key_from_base58(base58)

    fun bip32Fingerprint(bip32xPub: String): String {
        Wally.bip32_key_from_base58(bip32xPub).also { ext_key ->
            try {
                return Wally.hex_from_bytes(Wally.bip32_key_get_fingerprint(ext_key))
            } finally {
                Wally.bip32_key_free(ext_key)
            }
        }
    }

    fun bip85FromMnemonic(
        mnemonic: String,
        passphrase: String?,
        isTestnet: Boolean = false,
        index: Long = 0,
        numOfWords: Long = 12
    ): String {
        val seed512 = Wally.bip39_mnemonic_to_seed512(mnemonic, passphrase)

        val version = if(isTestnet) BIP32_VER_TEST_PRIVATE else BIP32_VER_MAIN_PRIVATE

        val bip32Key = Wally.bip32_key_from_seed(seed512, version.toLong() , Wally.BIP32_FLAG_SKIP_HASH.toLong())

        val bip85 = Wally.bip85_get_bip39_entropy(bip32Key, BIP39_WORD_LIST_LANG, numOfWords, index)

        return Wally.bip39_mnemonic_from_bytes(bip39WordList, bip85).also {
            Wally.bip32_key_free(bip32Key)
        }
    }
    
    companion object{
        const val BIP39_WORD_LIST_LANG = "en"
        const val WALLY_SECP_RANDOMIZE_LEN = Wally.WALLY_SECP_RANDOMIZE_LEN
    }

}