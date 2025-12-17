package com.blockstream.domain.swap

import com.blockstream.data.data.GreenWallet
import com.blockstream.data.data.SwapType
import com.blockstream.data.database.Database
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.swap.Quote
import com.blockstream.data.swap.SwapDetails
import com.blockstream.jade.Loggable

/**
 * Creates or restores a Chain Swap between different on-chain networks (e.g., BTC to Liquid or Liquid to BTC).
 *
 * A Chain Swap allows exchanging funds between Bitcoin and Liquid by using a lockup address on the
 * source network. The swap details are persisted in the database for tracking and subsequent
 * settlement or refunding by background processes.
 */
class CreateChainSwapUseCase(
    private val database: Database
) {
    /**
     * Executes the chain swap creation.
     *
     * This method interacts with the Liquid Wallet Kit (LWK) to generate the appropriate
     * swap lockup details and then persists these details in the local database.
     *
     * @param wallet the active [GreenWallet] identifying the user's wallet
     * @param session the current [GdkSession] providing access to the LWK and network details
     * @param fromAccount the source [Account] (Bitcoin or Liquid) that will fund the swap
     * @param fees optional [Fees] information used to calculate the expected receive amount
     * @param amount the amount to swap (in satoshis)
     * @param address the destination address on the target network where funds will be received
     * @return [SwapDetails] containing the lockup address, swap ID, and fee breakdown
     * @throws Exception if the account type is neither Bitcoin nor Liquid, or if session data is missing
     */
    suspend operator fun invoke(
        wallet: GreenWallet,
        session: GdkSession,
        fromAccount: Account,
        toAccount: Account,
        quote: Quote?,
        amount: Long,
        address: String
    ): SwapDetails {

        val xPubHashId = session.xPubHashId ?: throw Exception("xPubHashId should not be null")

        val lockup = if (fromAccount.isBitcoin) {
            require(toAccount.isLiquid)
            session.lwk.btcToLbtc(amount = amount, refundAddress = session.getReceiveAddress(fromAccount).address, claimAddress = address)
        } else if (fromAccount.isLiquid) {
            require(toAccount.isBitcoin)
            session.lwk.lbtcToBtc(amount = amount, refundAddress = session.getReceiveAddress(fromAccount).address, claimAddress = address)
        } else {
            throw Exception("Invalid account type")
        }

        database.setSwap(
            id = lockup.swapId(),
            walletId = wallet.id,
            xPubHashId = xPubHashId,
            swapType = SwapType.Chain,
            isAutoSwap = false,
            data = lockup.serialize()
        )
        
        return SwapDetails(
            swapId = lockup.swapId(),
            address = lockup.lockupAddress(),
            fromAmount = lockup.expectedAmount().toLong(),
            toAmount = quote?.receiveAmount,
            fromAssetId = fromAccount.network.policyAsset,
            toAssetId = toAccount.network.policyAsset,
            providerFee = quote?.boltzFee ?: 0,
            claimNetworkFee = quote?.claimNetworkFee ?: 0
        )
    }

    companion object : Loggable()
}
