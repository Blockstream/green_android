package com.blockstream.common.gdk

import co.touchlab.kermit.Logger
import com.blockstream.common.utils.getSecureRandom
import com.blockstream.common.utils.toHex
import gdk.BIP32_FLAG_SKIP_HASH
import gdk.BIP32_KEY_FINGERPRINT_LEN
import gdk.BIP32_VER_MAIN_PRIVATE
import gdk.BIP32_VER_TEST_PRIVATE
import gdk.BIP39_SEED_LEN_512
import gdk.BIP39_WORDLIST_LEN
import gdk.HMAC_SHA512_LEN
import gdk.WALLY_OK
import gdk.WALLY_SECP_RANDOMIZE_LEN
import gdk.bip32_key_from_base58
import gdk.bip32_key_from_seed
import gdk.bip32_key_get_fingerprint
import gdk.bip39_get_word
import gdk.bip39_mnemonic_from_bytes
import gdk.bip39_mnemonic_to_seed512
import gdk.bip39_mnemonic_validate
import gdk.bip85_get_bip39_entropy
import gdk.ext_key
import gdk.wally_free_string
import gdk.wally_init
import gdk.wally_secp_randomize
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.posix.size_tVar


class IOSWally : Wally {

    override val bip39TotalWords: Int
        get() = BIP39_WORDLIST_LEN


    init {
        wally_init((0).convert())

        getSecureRandom().randomBytes(WALLY_SECP_RANDOMIZE_LEN).toUByteArray().usePinned { random ->
            wally_secp_randomize(random.addressOf(0), random.get().size.convert())
        }
    }

    override fun bip39GetWord(index: Int): String {
        return memScoped {
            val word = allocPointerTo<ByteVar>()

            bip39_get_word(
                null,
                index.convert(),
                word.ptr
            )

            defer {
                wally_free_string(word.value)
            }

            word.value?.toKString() ?: ""
        }
    }

    override fun bip39MnemonicValidate(mnemonic: String): Boolean {
        Logger.d { "Wally bip39MnemonicValidate" }
        return bip39_mnemonic_validate(null, mnemonic) == WALLY_OK
    }

    override fun isXpubValid(xpub: String): Boolean {
        memScoped {
            val extKey = alloc<ext_key>()

            return bip32_key_from_base58(xpub, extKey.ptr) == WALLY_OK
        }
    }

    override fun bip32Fingerprint(bip32xPub: String): String {
        memScoped {
            val extKey = alloc<ext_key>()

            bip32_key_from_base58(bip32xPub, extKey.ptr)

            ByteArray(BIP32_KEY_FINGERPRINT_LEN).toUByteArray().usePinned { byteArray ->
                bip32_key_get_fingerprint(
                    extKey.ptr,
                    byteArray.addressOf(0),
                    byteArray.get().size.convert()
                )
                return byteArray.get().toHex()
            }
        }
    }

    override fun bip85FromMnemonic(
        mnemonic: String,
        passphrase: String?,
        isTestnet: Boolean,
        index: Long,
        numOfWords: Long
    ): String {
        Logger.d { "Wally bip85FromMnemonic" }
        memScoped {
            val seed512 = ByteArray(BIP39_SEED_LEN_512).toUByteArray()
            val bip32Key: ext_key = alloc()
            val version = if (isTestnet) BIP32_VER_TEST_PRIVATE else BIP32_VER_MAIN_PRIVATE
            val bip85 = ByteArray(HMAC_SHA512_LEN).toUByteArray()
            val bip85Mnemonic = allocPointerTo<ByteVar>()

            seed512.usePinned { byteArray ->
                val seed512Pointer = byteArray.addressOf(0)

                bip39_mnemonic_to_seed512(
                    mnemonic,
                    passphrase,
                    seed512Pointer,
                    BIP39_SEED_LEN_512.convert()
                )

                bip32_key_from_seed(
                    seed512Pointer,
                    BIP39_SEED_LEN_512.convert(),
                    version.convert(),
                    BIP32_FLAG_SKIP_HASH.convert(),
                    bip32Key.ptr
                )
            }

            val written: size_tVar = alloc()

            bip85.usePinned { byteArray ->
                val pointer = byteArray.addressOf(0)

                bip85_get_bip39_entropy(
                    bip32Key.ptr,
                    null,
                    numOfWords.convert(),
                    index.convert(),
                    pointer,
                    HMAC_SHA512_LEN.convert(),
                    written.ptr
                )

                bip39_mnemonic_from_bytes(
                    null,
                    pointer,
                    written.value.convert(),
                    bip85Mnemonic.ptr
                )

                defer {
                    wally_free_string(bip85Mnemonic.value)
                }

                return bip85Mnemonic.value?.toKString() ?: ""

            }
        }
    }
}

actual fun getWally(): Wally = IOSWally()