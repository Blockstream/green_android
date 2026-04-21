package com.blockstream.data.walletabi.provider

import com.blockstream.data.jade.JadeHWWallet
import lwk.Keypair
import lwk.LwkException
import lwk.Pset
import lwk.WalletAbiSignerCallbacks
import lwk.XOnlyPublicKey

@OptIn(ExperimentalStdlibApi::class)
class WalletAbiJadeSignerCallbacks(
    private val jadeWallet: JadeHWWallet,
) : WalletAbiSignerCallbacks {
    private val identityKeypair: Keypair by lazy {
        val keyBytes = jadeWallet.getWalletAbiSharedIdentityKey()
        Keypair.fromSecretBytes(keyBytes)
    }

    override fun getRawSigningXOnlyPubkey(): XOnlyPublicKey {
        return identityKeypair.xOnlyPublicKey()
    }

    override fun signPst(pst: Pset): Pset {
        throw LwkException.Generic(
            "Wallet ABI Jade PSET signing is not available in the Android Jade transport yet",
        )
    }

    override fun signSchnorr(message: ByteArray): ByteArray {
        if (message.size != 32) {
            throw LwkException.Generic("Wallet ABI Schnorr message must be 32 bytes")
        }
        return identityKeypair.signSchnorr(message.toHexString()).hexToByteArray()
    }
}
