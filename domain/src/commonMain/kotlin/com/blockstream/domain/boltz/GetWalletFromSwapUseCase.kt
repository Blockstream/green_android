package com.blockstream.domain.boltz

import com.blockstream.data.data.GreenWallet
import com.blockstream.data.database.Database
import com.blockstream.data.database.wallet.BoltzSwaps
import com.blockstream.jade.Loggable

/**
 * Looks up a stored swap and resolves the associated wallet, if any.
 *
 * This is a lightweight helper used by background tasks to determine which wallet a swap belongs
 * to. If the wallet row is not directly available, it falls back to searching by xpub hash id for
 * a mainnet wallet.
 */
class GetWalletFromSwapUseCase(
    private val database: Database
) : Loggable() {

    /**
     * Resolves the swap and its wallet for the provided [swapId].
     *
     * @param swapId the identifier saved alongside the swap
     * @return a [Pair] of the [BoltzSwaps] row (may be null) and the [GreenWallet] (may be null)
     */
    suspend operator fun invoke(
        swapId: String
    ): Pair<BoltzSwaps?, GreenWallet?> {

        val swap = database.getSwap(id = swapId)
        val wallet =
            swap?.let { database.getWallet(id = it.wallet_id) ?: database.getMainnetWalletWithXpubHashId(xPubHashId = it.xpub_hash_id) }

        return swap to wallet
    }
}
