package com.blockstream.domain.receive

import com.blockstream.data.CountlyBase
import com.blockstream.data.data.Denomination
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.lightning.maxReceivableSatoshi
import com.blockstream.data.lightning.totalInboundLiquiditySatoshi
import com.blockstream.data.receive.LightningReceiveAmountState
import com.blockstream.data.utils.toAmountLook
import com.blockstream.data.utils.UserInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetLightningReceiveAmountStateUseCase(
    val countly: CountlyBase
) {
    operator fun invoke(
        session: GdkSession,
        amountFlow: Flow<String>,
        isReverseSubmarineSwapFlow: Flow<Boolean>,
        showLightningOnChainAddressFlow: Flow<Boolean>,
        accountAsset: AccountAsset,
        denominationFlow: Flow<Denomination>
    ): Flow<LightningReceiveAmountState> = combine(
        amountFlow,
        isReverseSubmarineSwapFlow,
        showLightningOnChainAddressFlow,
        denominationFlow
    ) { amountText, isSwap, showOnChain, denomination ->

        val isLightningInvoiceFlow = (accountAsset.account.isLightning && !showOnChain) && !isSwap

        if (!isLightningInvoiceFlow) {
            return@combine LightningReceiveAmountState.None
        }

        if (amountText.isBlank()) {
            return@combine LightningReceiveAmountState.None
        }

        val amountSatoshi = UserInput.parseUserInputSafe(
            session = session,
            input = amountText,
            denomination = denomination,
            assetId = accountAsset.asset.assetId
        ).getBalance()?.satoshi ?: return@combine LightningReceiveAmountState.Error.InvalidAmount

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
        // e.g. amountSatoshi (5_000_000) > lnMaxSatoshis (4_000_000) -> blocks confirm button
        if (amountSatoshi > lnMaxSatoshis) {
            return@combine LightningReceiveAmountState.Error.AmountTooHigh(
                maxAmountStr = maxAmountStr,
                maxFiatStr = maxFiatStr
            )
        }
        // CASE 2: No active channels and below network floor
        // e.g. has no channels, amountSatoshi (500) < lnMinSatoshis (1_000) -> blocks confirm button
        if (!hasChannels && amountSatoshi < lnMinSatoshis) {
            return@combine LightningReceiveAmountState.Error.AmountTooLow(
                minAmountStr = minAmountStr,
                minFiatStr = minFiatStr
            )
        }

        // CASE 3: Active channels exist but amount requires an on-the-fly channel lease
        // e.g. has channels, amountSatoshi (150_000) > maxReceivableSats (100_000) -> prompts funding fee warning
        if (hasChannels && amountSatoshi > maxReceivableSats) {
            return@combine if (amountSatoshi < lnRecommendedSatoshis) {
                LightningReceiveAmountState.Recommend(satsStr = recAmountStr)
            } else {
                LightningReceiveAmountState.Info
            }
        }

        // CASE 4: New wallet needs its very first channel setup
        // e.g. has no channels, amountSatoshi (50_000) passed Case 2 floor -> prompts initial funding fee warning.
        if (!hasChannels) {
            return@combine if (amountSatoshi < lnRecommendedSatoshis) {
                LightningReceiveAmountState.Recommend(satsStr = recAmountStr)
            } else {
                LightningReceiveAmountState.Info
            }
        }

        LightningReceiveAmountState.None
    }
}