package com.blockstream.domain.receive

import com.blockstream.data.CountlyBase
import com.blockstream.data.data.Denomination
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.lightning.maxReceivableSatoshi
import com.blockstream.data.lightning.totalInboundLiquiditySatoshi
import com.blockstream.data.receive.FeeCommunicationState
import com.blockstream.data.utils.toAmountLook
import com.blockstream.data.utils.UserInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetFeeCommunicationStateUseCase(
    val countly: CountlyBase
) {
    operator fun invoke(
        session: GdkSession,
        amountFlow: Flow<String>,
        isReverseSubmarineSwapFlow: Flow<Boolean>,
        showLightningOnChainAddressFlow: Flow<Boolean>,
        accountAsset: AccountAsset,
        denominationFlow: Flow<Denomination>
    ): Flow<FeeCommunicationState> = combine(
        amountFlow,
        isReverseSubmarineSwapFlow,
        showLightningOnChainAddressFlow,
        denominationFlow
    ) { amountText, isSwap, showOnChain, denomination ->

        val isLightningInvoiceFlow = (accountAsset.account.isLightning && !showOnChain) && !isSwap

        if (!isLightningInvoiceFlow) {
            return@combine FeeCommunicationState.None
        }

        if (amountText.isBlank()) {
            return@combine FeeCommunicationState.None
        }

        val userInput = UserInput.parseUserInputSafe(
            session = session,
            input = amountText,
            denomination = denomination,
            assetId = accountAsset.asset.assetId
        )

        val cleanIntegerPart = userInput.amount.takeWhile { it != '.' }
        val isSatsUnit = denomination.denomination == com.blockstream.data.SATOSHI_UNIT

        val isOverflow = if (isSatsUnit) {
            cleanIntegerPart.length > MAX_SATS_INTEGER_LENGTH
        } else {
            cleanIntegerPart.length > MAX_BTC_OR_FIAT_INTEGER_LENGTH
        }

        if (isOverflow || userInput.amountAsDouble < 0.0) {
            return@combine FeeCommunicationState.Error.InvalidAmount
        }

        val amountSats = userInput.getBalance()?.satoshi ?: 0L

        val lnMinSatoshis = countly.getLnMinSatoshis()
        val lnRecommendedSatoshis = countly.getLnRecommendedSatoshis()
        val lnMaxSatoshis = countly.getLnMaxSatoshis()

        val nodeInfo = session.lightningSdkOrNull?.nodeInfoStateFlow?.value
        val maxReceivableSats = if (accountAsset.account.isLightning && nodeInfo != null) {
            nodeInfo.maxReceivableSatoshi()
        } else {
            0L
        }

        val totalInboundLiquiditySatoshi = nodeInfo?.totalInboundLiquiditySatoshi() ?: 0L
        val hasChannels = totalInboundLiquiditySatoshi > 0L

        val maxAmountStr = lnMaxSatoshis.toAmountLook(session = session, assetId = accountAsset.asset.assetId, denomination = denomination, withUnit = true) ?: "$lnMaxSatoshis sats"
        val maxFiatStr = lnMaxSatoshis.toAmountLook(session = session, assetId = accountAsset.asset.assetId, denomination = Denomination.fiat(session), withUnit = true) ?: ""
        val minAmountStr = lnMinSatoshis.toAmountLook(session = session, assetId = accountAsset.asset.assetId, denomination = denomination, withUnit = true) ?: "$lnMinSatoshis sats"
        val minFiatStr = lnMinSatoshis.toAmountLook(session = session, assetId = accountAsset.asset.assetId, denomination = Denomination.fiat(session), withUnit = true) ?: ""
        val recAmountStr = lnRecommendedSatoshis.toAmountLook(session = session, assetId = accountAsset.asset.assetId, denomination = denomination, withUnit = true) ?: "$lnRecommendedSatoshis sats"

        // CASE 1: Hard limit overflow
        // e.g. amountSats (5_000_000) > lnMaxSatoshis (4_000_000) -> blocks confirm button
        if (amountSats > lnMaxSatoshis) {
            return@combine FeeCommunicationState.Error.AmountTooHigh(
                maxAmountStr = maxAmountStr,
                maxFiatStr = maxFiatStr
            )
        }
        // CASE 2: No active channels and below network floor
        // e.g. has no channels, amountSats (500) < lnMinSatoshis (1_000) -> blocks confirm button
        if (!hasChannels && amountSats < lnMinSatoshis) {
            return@combine FeeCommunicationState.Error.AmountTooLow(
                minAmountStr = minAmountStr,
                minFiatStr = minFiatStr
            )
        }

        // CASE 3: Active channels exist but amount requires an on-the-fly channel lease
        // e.g. has channels, amountSats (150_000) > maxReceivableSats (100_000) -> prompts funding fee warning
        if (hasChannels && amountSats > maxReceivableSats) {
            return@combine if (amountSats < lnRecommendedSatoshis) {
                FeeCommunicationState.Recommend(satsStr = recAmountStr)
            } else {
                FeeCommunicationState.Info
            }
        }

        // CASE 4: New wallet needs its very first channel setup
        // e.g. has no channels, amountSats (50_000) passed Case 2 floor -> prompts initial funding fee warning.
        if (!hasChannels) {
            return@combine if (amountSats < lnRecommendedSatoshis) {
                FeeCommunicationState.Recommend(satsStr = recAmountStr)
            } else {
                FeeCommunicationState.Info
            }
        }

        FeeCommunicationState.None
    }

    companion object {
        private const val MAX_SATS_INTEGER_LENGTH = 15
        private const val MAX_BTC_OR_FIAT_INTEGER_LENGTH = 8
    }
}