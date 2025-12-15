package com.blockstream.domain.receive

import com.blockstream.data.data.Denomination
import com.blockstream.data.extensions.isBlank
import com.blockstream.data.extensions.isNotBlank
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.lightning.feeSatoshi
import com.blockstream.data.lightning.maxReceivableSatoshi
import com.blockstream.data.lightning.totalInboundLiquiditySatoshi
import com.blockstream.data.lwk.BoltzLimits
import com.blockstream.data.utils.UserInput
import com.blockstream.data.utils.toAmountLook
import com.blockstream.data.utils.toAmountLookOrNa
import com.github.michaelbull.retry.policy.constantDelay
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.policy.stopAtAttempts
import com.github.michaelbull.retry.retry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Use case that determines which assets are relevant for a send flow given a raw input string.
 *
 * Input can be a variety of schemes (e.g., on-chain Bitcoin, Liquid, or Lightning). The logic:
 * - If the parsed network is Bitcoin or Lightning, returns a single `EnrichedAsset` for the
 *   network's `policyAsset` (i.e., the native asset for that network).
 * - If the parsed network is Liquid and the input contains an `assetid` query parameter, returns
 *   that specific asset.
 * - If the parsed network is Liquid and no `assetid` is present, returns all positive-balance
 *   assets from the user's wallet that belong to the Liquid network.
 *
 * Notes on query parsing:
 * - This use case uses a safe query-parameter extraction strategy to support opaque URIs
 *   (e.g., `liquidnetwork:lq1...?...`) that are not hierarchical. See `getSafeQueryParameter`
 *   and `UriUtils.getQueryParameter` for details.
 *
 * Errors:
 * - If the input cannot be parsed into a supported network/address, the operation throws with
 *   message `id_invalid_address`.
 */

data class ReceiveAmountData(
    val isValid: Boolean = false,
    val exchange: String = "",
    val liquidityFee: String? = null,
    val hint: String? = null,
    val error: String? = null
)

class GetReceiveAmountUseCase(private val session: GdkSession, private val accountAsset: AccountAsset) {
    private var limits: MutableStateFlow<BoltzLimits?> = MutableStateFlow<BoltzLimits?>(null)
    private val scope = CoroutineScope(context = Dispatchers.Default)

    init {
        scope.launch {
            tryCatch {
                retry(stopAtAttempts<Throwable>(3) + constantDelay<Throwable>(delayMillis = 5000L)) {
                    limits.value = session.lwkOrNull?.fetchSwapsInfo()
                }
            }
        }
    }

    operator fun invoke(
        amount: StateFlow<String>,
        denomination: StateFlow<Denomination>,
        isReverseSubmarineSwap: StateFlow<Boolean>
    ): Flow<ReceiveAmountData> {

        return combine(
            amount,
            denomination,
            isReverseSubmarineSwap, limits,
            session.lightningSdkOrNull?.nodeInfoStateFlow ?: flowOf(null)
        ) { amount, denomination, isReverseSubmarineSwap, swapLimits, nodeInfo ->

            var response = ReceiveAmountData()

            val balance = amount.takeIf { it.isNotBlank() }?.let {
                UserInput.parseUserInputSafe(
                    session = session, input = it, assetId = accountAsset.asset.assetId, denomination = denomination
                ).getBalance()
            }?.also {
                response = response.copy(
                    exchange = "≈ " + it.toAmountLook(
                        session = session, assetId = accountAsset.asset.assetId, denomination = Denomination.exchange(
                            session = session, denomination = denomination
                        ), withUnit = true, withGrouping = true, withMinimumDigits = false
                    )
                )
            }

            if (accountAsset.account.isLightning) {
                if (nodeInfo != null) {
                    val openChannelFee = balance?.satoshi?.let {
                        if (it > nodeInfo.totalInboundLiquiditySatoshi()) session.lightningSdk.openChannelFee(
                            it
                        ) else null
                    }

                    val isValid =
                        balance != null && (balance.satoshi >= 0 && balance.satoshi <= nodeInfo.maxReceivableSatoshi() && (balance.satoshi <= nodeInfo.totalInboundLiquiditySatoshi() || (balance.satoshi > (openChannelFee?.feeSatoshi()
                            ?: 0))))

                    val hint = nodeInfo.maxReceivableSatoshi().toAmountLook(
                        session = session,
                        assetId = session.lightningAccount.network.policyAsset,
                        denomination = denomination,
                        withUnit = true
                    )?.let {
                        "id_max_limit_s|$it"
                    }

                    val error = if (amount.isBlank()) null else {
                        if (balance != null) {
                            val inboundLiquidity = nodeInfo.totalInboundLiquiditySatoshi()
                            val channelMinimum = openChannelFee?.feeSatoshi() ?: 0
                            if (balance.satoshi > inboundLiquidity) {
                                "id_the_amount_is_above_your_inbound|${
                                    inboundLiquidity.toAmountLook(
                                        session = session, withUnit = true, denomination = denomination.notFiat()
                                    ) ?: ""
                                }|${
                                    inboundLiquidity.toAmountLook(
                                        session = session, withUnit = true, denomination = Denomination.fiat(session)
                                    ) ?: ""
                                }"
                            } else if (balance.satoshi <= channelMinimum) {
                                "id_this_amount_is_below_the|${
                                    channelMinimum.toAmountLook(
                                        session = session, withUnit = true, denomination = denomination.notFiat()
                                    ) ?: ""
                                }|${
                                    channelMinimum.toAmountLook(
                                        session = session, withUnit = true, denomination = Denomination.fiat(session)
                                    ) ?: ""
                                }"
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }

                    val isSetupChannel = nodeInfo.totalInboundLiquiditySatoshi() == 0L

                    val channelFee = openChannelFee?.feeSatoshi()?.toAmountLook(
                        session = session,
                        assetId = accountAsset.account.network.policyAsset,
                        denomination = denomination.notFiat(),
                        withUnit = true
                    ) ?: "-"

                    val channelFeeFiat = openChannelFee?.feeSatoshi()?.toAmountLook(
                        session = session,
                        assetId = accountAsset.account.network.policyAsset,
                        denomination = Denomination.fiat(session),
                        withUnit = true
                    ) ?: "-"

                    val liquidityFee = when {
                        amount.isBlank() || error != null -> {
                            null
                        }

                        isSetupChannel -> {
                            "id_a_set_up_funding_fee_of_s_s|$channelFee|$channelFeeFiat"
                        }

                        (balance?.satoshi ?: 0) > nodeInfo.totalInboundLiquiditySatoshi() -> {

                            val inboundLiquidity = nodeInfo.totalInboundLiquiditySatoshi().toAmountLookOrNa(
                                session = session,
                                assetId = session.lightningAccount.network.policyAsset,
                                denomination = denomination.notFiat(),
                                withUnit = true
                            )

                            val inboundLiquidityFiat = nodeInfo.totalInboundLiquiditySatoshi().toAmountLook(
                                session = session,
                                assetId = session.lightningAccount.network.policyAsset,
                                denomination = Denomination.fiat(session),
                                withUnit = true
                            ) ?: ""

                            "id_a_funding_fee_of_s_s_is_applied|$channelFee|$channelFeeFiat|$inboundLiquidity|$inboundLiquidityFiat"
                        }

                        else -> null
                    }

                    response.copy(isValid = isValid, hint = hint, liquidityFee = liquidityFee, error = error)
                } else {
                    response
                }
            } else if (accountAsset.account.isLiquid && isReverseSubmarineSwap) {

                val isValid = balance != null && balance.satoshi > 0 && (balance.satoshi >= (swapLimits?.limits?.minimal
                    ?: 0)) && (balance.satoshi <= (swapLimits?.limits?.maximal ?: Long.MAX_VALUE))

                val hint = when {
                    balance == null -> null
                    balance.satoshi >= (swapLimits?.limits?.minimal ?: 0) -> {
                        swapLimits?.limits?.maximal.toAmountLook(
                            session = session,
                            assetId = accountAsset.account.network.policyAsset,
                            denomination = denomination,
                            withUnit = true
                        )?.let {
                            "id_max_limit_s|$it"
                        }
                    }

                    else -> {
                        swapLimits?.limits?.minimal.toAmountLook(
                            session = session,
                            assetId = accountAsset.account.network.policyAsset,
                            denomination = denomination,
                            withUnit = true
                        )?.let {
                            "id_min_limit_s|$it"
                        }
                    }
                }

                val error = when {
                    balance == null -> null
                    balance.satoshi < (swapLimits?.limits?.minimal ?: 0) -> {
                        swapLimits?.limits?.minimal.toAmountLook(
                            session = session,
                            assetId = accountAsset.account.network.policyAsset,
                            denomination = denomination,
                            withUnit = true
                        )?.let {
                            "id_min_limit_s|$it"
                        }
                    }

                    balance.satoshi > (swapLimits?.limits?.maximal ?: Long.MAX_VALUE) -> {
                        swapLimits?.limits?.maximal.toAmountLook(
                            session = session,
                            assetId = accountAsset.account.network.policyAsset,
                            denomination = denomination,
                            withUnit = true
                        )?.let {
                            "id_max_limit_s|$it"
                        }
                    }

                    else -> null
                }

                response.copy(isValid = isValid, hint = hint, error = error)
            } else {
                val isValid = balance != null && balance.satoshi > 0
                response.copy(isValid = isValid)
            }
        }
    }
}
