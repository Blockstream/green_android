package com.blockstream.domain.send

import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.tryCatch
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.params.AddressParams
import com.blockstream.common.gdk.params.CreateTransactionParams
import com.blockstream.common.gdk.params.toJsonElement
import com.blockstream.common.utils.UserInput
import com.blockstream.domain.boltz.BoltzUseCase
import com.blockstream.green.data.lwk.NormalSubmarineSwap
import com.blockstream.jade.Loggable
import kotlinx.serialization.json.buildJsonObject

/**
 * Builds the `CreateTransactionParams` used to preview or create a transaction for a given
 * `AccountAsset`, handling both on-chain and Lightning flows as well as Liquid submarine swaps.
 *
 * Behavior overview
 * - Lightning account (Greenlight/Breez):
 *   - Parses the `amount` using the provided `denomination` and sets `satoshi` accordingly.
 *   - Creates a simple addressee list with the LN invoice/address and a dummy UTXO entry to force
 *     transaction re-calculation when balance changes (hack for refreshing params).
 *
 * - On-chain accounts:
 *   - If the network is Liquid and the destination is a Lightning invoice and swaps are enabled,
 *     a Normal Submarine Swap is created through `BoltzUseCase`. The on-chain transaction then
 *     pays to the swap address for the required `satoshi` amount.
 *   - Otherwise, builds a normal on-chain payment with optional "send all" (`isGreedy`).
 *   - Fee-rate and UTXO set are propagated so the caller can control fee selection and coin
 *     selection. For Liquid, the asset id is attached to the addressee.
 *
 * Notes
 * - The `amount` is parsed via `UserInput.parseUserInputSafe`. When not in send-all mode, if the
 *   parsed balance is out of range, `satoshi` may be `null` and is treated as `0` here; upstream
 *   validation should handle insufficient amounts.
 * - For Lightning accounts we include a `utxos` map with the policy asset to force param refresh.
 * - This use case does not broadcast; it only prepares parameters for transaction creation.
 */
class PrepareTransactionUseCase(private val boltzUseCase: BoltzUseCase) {

    /**
     * Prepare a `CreateTransactionParams` instance for the given destination and amount.
     *
     * Parameters
     * - `greenWallet`: The wallet context used for feature flags (e.g., swaps) and services.
     * - `session`: Active `GdkSession` providing account state, parsing and UTXOs.
     * - `accountAsset`: The source account and asset for the transaction.
     * - `address`: Destination address or Lightning invoice. For Liquid + invoice, a swap may be created.
     * - `amount`: User-entered amount string to parse (e.g., "0.01", "10000").
     * - `denomination`: Optional denomination to interpret the amount (BTC, sats, fiat…).
     * - `isSendAll`: When true (on-chain), marks the transaction as greedy to spend all selectable funds.
     * - `feeRate`: Optional fee rate (sats/vB or policy-specific) to guide fee selection.
     *
     * Returns
     * - A fully populated `CreateTransactionParams` suitable for passing to the creation/preview layer.
     */
    suspend operator fun invoke(
        greenWallet: GreenWallet,
        session: GdkSession,
        accountAsset: AccountAsset,
        address: String,
        amount: String? = null,
        denomination: Denomination? = null,
        isSendAll: Boolean = false,
        feeRate: Long? = null,
    ): CreateTransactionParams {

        return (if (accountAsset.account.network.isLightning) {
            val satoshi = UserInput.parseUserInputSafe(
                session = session,
                input = amount,
                denomination = denomination
            ).getBalance(onlyInAcceptableRange = false)?.satoshi

            AddressParams(
                address = address,
                satoshi = satoshi ?: 0
            ).let { params ->
                CreateTransactionParams(
                    addressees = listOf(params).toJsonElement(),
                    utxos = mapOf(BTC_POLICY_ASSET to listOf(buildJsonObject {
                        // a hack to re-create params when balance changes
                        session.accountAssets(accountAsset.account).value.policyAsset
                    }))
                )
            }
        } else {
            val isGreedy: Boolean
            val satoshi: Long?
            val toAddress: String
            var swap: NormalSubmarineSwap? = null

            val parsedAddress = session.parseInput(address)

            val isSwap = if (accountAsset.account.network.isLiquid) {
                tryCatch { parsedAddress?.first?.isLightning } ?: false && boltzUseCase.isSwapsEnabledUseCase(wallet = greenWallet)
            } else false

            if (isSwap) {

                swap = boltzUseCase.createNormalSubmarineSwapUseCase(
                    wallet = greenWallet,
                    session = session,
                    account = accountAsset.account,
                    invoice = address
                )
                logger.d { "Swap: $swap" }

                isGreedy = false
                toAddress = swap.address
                satoshi = swap.satoshi
            } else {
                isGreedy = isSendAll
                toAddress = address
                satoshi = if (isGreedy) 0 else UserInput.parseUserInputSafe(
                    session = session,
                    input = amount,
                    assetId = accountAsset.assetId,
                    denomination = denomination
                ).getBalance(onlyInAcceptableRange = false)?.satoshi
            }

            val unspentOutputs = accountAsset.account.let { session.getUnspentOutputs(it) }

            AddressParams(
                address = toAddress,
                satoshi = satoshi ?: 0,
                isGreedy = isGreedy,
                assetId = accountAsset.assetId.takeIf { accountAsset.account.network.isLiquid }
            ).let { params ->
                CreateTransactionParams(
                    from = accountAsset,
                    addressees = listOf(params).toJsonElement(),
                    feeRate = feeRate,
                    utxos = unspentOutputs.unspentOutputs,
                    submarineSwap = swap
                )
            }
        })
    }

    companion object : Loggable()
}
