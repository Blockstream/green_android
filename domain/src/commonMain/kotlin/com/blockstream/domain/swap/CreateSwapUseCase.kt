package com.blockstream.domain.swap

import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.lightning.satoshi
import com.blockstream.data.swap.Quote
import com.blockstream.data.swap.SwapDetails

/**
 * Orchestrates the creation of various swap types: Chain, Normal Submarine, or Reverse Submarine.
 *
 * This use case serves as a router that selects and delegates to the appropriate specialized
 * swap use case based on the combination of source (from) and destination (to) networks.
 */
class CreateSwapUseCase(
    private val createChainSwapUseCase: CreateChainSwapUseCase,
    private val createReverseSubmarineSwapUseCase: CreateReverseSubmarineSwapUseCase,
    private val createNormalSubmarineSwapUseCase: CreateNormalSubmarineSwapUseCase
) {

    /**
     * Executes the swap creation by determining the swap type based on account properties.
     *
     * Routing Rules:
     * - **Chain Swap**: Used when both source and destination are on-chain (Bitcoin or Liquid).
     * - **Normal Submarine Swap**: Used when swapping from Liquid to Lightning. An invoice is
     *   internally generated and then prepared for payment via an on-chain Liquid transaction.
     * - **Reverse Submarine Swap**: Used when swapping from Lightning to Liquid. A BOLT11 invoice
     *   is created for the user to pay from their Lightning balance.
     *
     * @param wallet the active [GreenWallet] identifying the user's wallet
     * @param session the current [GdkSession]
     * @param from the source [AccountAsset] (where funds come from)
     * @param to the destination [AccountAsset] (where funds will go)
     * @param fees optional [Fees] information for calculation (mainly for Chain and Reverse swaps)
     * @param amount the amount to swap (in satoshis)
     * @return [SwapDetails] containing the necessary information to proceed with the swap
     * @throws Exception if the networks are identical or if the requested swap pair is not supported
     */
    suspend operator fun invoke(
        wallet: GreenWallet,
        session: GdkSession,
        from: AccountAsset,
        to: AccountAsset,
        quote: Quote?,
        amount: Long?
    ): SwapDetails {

        if (from.account.network.isSameNetwork(to.account.network)) {
            throw Exception("Network must be different to do a swap")
        }

        val amountNotNull = requireNotNull(amount) { "Amount is required for swap creation" }

        return when {
            // Chain
            listOf(from.account.isLightning, to.account.isLightning).all { !it } -> {

                createChainSwapUseCase(
                    wallet = wallet,
                    session = session,
                    fromAccount = from.account,
                    toAccount = to.account,
                    quote = quote,
                    amount = amountNotNull,
                    address = session.getReceiveAddress(to.account).address
                )
            }

            from.account.isLiquid && to.account.isLightning -> {

                val invoice = session.createLightningInvoice(satoshi = amountNotNull, description = "")

                createNormalSubmarineSwapUseCase(
                    wallet = wallet,
                    session = session,
                    isAutoSwap = false,
                    account = from.account,
                    invoice = invoice.lnInvoice.bolt11
                )
            }

            from.account.isLightning && to.account.isLiquid -> {

                createReverseSubmarineSwapUseCase(
                    wallet = wallet,
                    session = session,
                    isAutoSwap = false,
                    account = from.account,
                    amount = amountNotNull
                ).let { swap ->
                    SwapDetails(
                        swapId = swap.swapId(),
                        address = swap.bolt11Invoice().toString(),
                        fromAmount = swap.bolt11Invoice().amountMilliSatoshis()!!.satoshi(),
                        toAmount = quote?.receiveAmount,
                        fromAssetId = from.account.network.policyAsset,
                        toAssetId = to.account.network.policyAsset,
                        providerFee = quote?.boltzFee ?: 0,
                        claimNetworkFee = quote?.claimNetworkFee ?: 0,
                    )
                }
            }

            else -> throw Exception("Invalid swap from $from to $to")
        }
    }
}
