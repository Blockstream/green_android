package com.blockstream.domain.swap

import com.blockstream.data.BTC_POLICY_ASSET
import com.blockstream.data.data.Denomination
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.params.AddressParams
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.gdk.params.toJsonElement
import com.blockstream.data.swap.Quote
import com.blockstream.data.utils.UserInput
import com.blockstream.jade.Loggable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject

/**
 * Prepares the parameters required to create a GDK transaction for a swap.
 *
 * This use case handles the initial steps of a swap: parsing the user amount, fetching swap limits,
 * creating the swap via LWK/Boltz, and constructing the [CreateTransactionParams] needed for
 * GDK transaction creation.
 */
class PrepareSwapTransactionUseCase(
    private val createSwapUseCase: CreateSwapUseCase
) {

    /**
     * Prepares the [CreateTransactionParams] for a swap.
     *
     * @param greenWallet the active [GreenWallet]
     * @param session the current [GdkSession]
     * @param from the source [AccountAsset]
     * @param to the destination [AccountAsset]
     * @param amount the amount string entered by the user
     * @param denomination optional [Denomination] for parsing the amount
     * @param feeRate optional custom fee rate for the on-chain transaction
     * @return [CreateTransactionParams] containing swap details and recipient info
     * @throws IllegalStateException if amount parsing fails
     */
    suspend operator fun invoke(
        greenWallet: GreenWallet,
        session: GdkSession,
        from: AccountAsset,
        to: AccountAsset,
        amount: String,
        denomination: Denomination? = null,
        quote: Quote? = null,
        feeRate: Long? = null,
    ): CreateTransactionParams {

        val satoshi = UserInput.parseUserInputSafe(
            session = session,
            input = amount,
            denomination = denomination,
            assetId = from.assetId
        ).getBalance(onlyInAcceptableRange = !from.account.isLightning)?.satoshi

        checkNotNull(satoshi)

        val swap = createSwapUseCase(
            wallet = greenWallet,
            session = session,
            from = from,
            to = to,
            quote = quote,
            amount = satoshi
        )

        val addressParams = AddressParams(
            address = swap.address,
            satoshi = satoshi,
            assetId = from.assetId.takeIf { from.account.network.isLiquid }
        )

        val utxos: Map<String, List<JsonElement>> = if (from.account.isLightning) {
            mapOf(BTC_POLICY_ASSET to listOf(buildJsonObject {
                // a hack to re-create params when balance changes
                session.accountAssets(from.account).value.policyAsset
            }))
        } else {
            session.getUnspentOutputs(from.account).unspentOutputs
        }

        return CreateTransactionParams(
            from = from,
            to = to,
            addressees = listOf(addressParams).toJsonElement(),
            feeRate = feeRate,
            utxos = utxos,
            swap = swap,
            isSwap = true
        )
    }

    companion object : Loggable()
}
