package com.blockstream.data.gdk

import com.blockstream.data.gdk.Wally.Companion.BIP39_WORD_LIST_LANG
import com.blockstream.data.utils.getSecureRandom
import com.blockstream.data.utils.toHex
import com.blockstream.libwally.Wally.WALLY_PSBT_VERSION_0
import com.blockstream.libwally.Wally as WallyJava

inline fun <T, R> T.bip32KeyFree(use: (T) -> R): R {
    try {
        return use(this)
    } catch (e: Throwable) {
        throw e
    } finally {
        WallyJava.bip32_key_free(this)
    }
}

class AndroidWally : Wally {
    override val aesBlockLen: Int = WallyJava.AES_BLOCK_LEN
    override val hmacSha256Len: Int = WallyJava.HMAC_SHA256_LEN
    override val ecPrivateKeyLen: Int = WallyJava.EC_PRIVATE_KEY_LEN
    override val ecSignatureRecoverableLen: Int = WallyJava.EC_SIGNATURE_RECOVERABLE_LEN
    override val bip39TotalWords: Int = WallyJava.BIP39_WORDLIST_LEN
    override val blindingFactorLen: Int = WallyJava.BLINDING_FACTOR_LEN

    private val bip39WordList by lazy { WallyJava.bip39_get_wordlist(BIP39_WORD_LIST_LANG) }

    init {
        WallyJava.init(0)
        WallyJava.secp_randomize(getSecureRandom().randomBytes(WallyJava.WALLY_SECP_RANDOMIZE_LEN))
    }

    override fun ecPrivateKeyVerify(privateKey: ByteArray): Boolean = try {
        WallyJava.ec_private_key_verify(privateKey)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun ecSigToDer(signature: ByteArray): String {
        return WallyJava.ec_sig_to_der(signature).toHexString()
    }

    override fun bip39GetWord(index: Int): String {
        return WallyJava.bip39_get_word(bip39WordList, index.toLong())
    }

    override fun bip39MnemonicValidate(mnemonic: String): Boolean {
        return try {
            WallyJava.bip39_mnemonic_validate(bip39WordList, mnemonic)
            true
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            false
        }
    }

    override fun isXpubValid(xpub: String): Boolean {
        try {
            WallyJava.bip32_key_from_base58(xpub)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    override fun bip32Fingerprint(bip32xPub: String): String? {
        return try {
            val bip32Key = WallyJava.bip32_key_from_base58(bip32xPub)
            return WallyJava.bip32_key_get_fingerprint(bip32Key).toHex()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun hashPrevouts(
        txHashes: ByteArray,
        utxoIndexes: List<Int>
    ): ByteArray {
        return WallyJava.get_hash_prevouts(txHashes, utxoIndexes.toIntArray())
    }

    override fun recoveryXpubBranchDerivation(
        recoveryXpub: String,
        branch: Long
    ): String {
        val accountKey =
            WallyJava.bip32_key_from_base58(recoveryXpub)

        val branchKey = WallyJava.bip32_key_from_parent(
            accountKey,
            branch,
            (WallyJava.BIP32_FLAG_KEY_PUBLIC or WallyJava.BIP32_FLAG_SKIP_HASH).toLong()
        )

        return WallyJava.bip32_key_to_base58(branchKey, WallyJava.BIP32_FLAG_KEY_PUBLIC.toLong())
            .also {
                WallyJava.bip32_key_free(accountKey)
                WallyJava.bip32_key_free(branchKey)
            }
    }

    override fun bip85FromMnemonic(
        mnemonic: String,
        passphrase: String?,
        isTestnet: Boolean,
        index: Long,
        numOfWords: Long
    ): String? {
        return try {
            val seed512 = WallyJava.bip39_mnemonic_to_seed512(mnemonic, passphrase)
            val version =
                if (isTestnet) WallyJava.BIP32_VER_TEST_PRIVATE else WallyJava.BIP32_VER_MAIN_PRIVATE

            val bip32Key = WallyJava.bip32_key_from_seed(
                seed512,
                version.toLong(),
                WallyJava.BIP32_FLAG_SKIP_HASH.toLong()
            )

            val bip85 = WallyJava.bip85_get_bip39_entropy(
                bip32Key,
                BIP39_WORD_LIST_LANG,
                numOfWords,
                index
            )

            WallyJava.bip39_mnemonic_from_bytes(bip39WordList, bip85)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun bip85FromJade(
        privateKey: ByteArray,
        publicKey: ByteArray,
        label: String,
        payload: ByteArray
    ): String? {
        return try {
            val out = ByteArray(payload.size)

            val written = WallyJava.aes_cbc_with_ecdh_key(
                privateKey,
                null,
                payload,
                publicKey,
                label.encodeToByteArray(),
                WallyJava.AES_FLAG_DECRYPT.toLong(),
                out
            )

            out.sliceArray(0 until written).let {
                WallyJava.bip39_mnemonic_from_bytes(bip39WordList, it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun psbtIsBase64(psbt: String): Boolean {
        return try {
            WallyJava.psbt_from_base64(psbt)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun psbtIsBinary(psbt: ByteArray): Boolean {
        return try {
            WallyJava.psbt_from_bytes(psbt)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun psbtToV0(psbt: String): String {
        val psbt = WallyJava.psbt_from_base64(psbt)

        WallyJava.psbt_set_version(psbt, 0, WALLY_PSBT_VERSION_0.toLong())

        return WallyJava.psbt_to_base64(psbt, 0)
    }
}

actual fun getWally(): Wally = AndroidWally()