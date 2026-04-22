package com.blockstream.domain.send

import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.data.EnrichedAssetList
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.gdk.GdkSession
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.AccountAssetBalanceList
import com.blockstream.data.data.Denomination
import com.blockstream.data.lightning.LightningInputType
import com.blockstream.data.lightning.maxPayableSatoshi
import com.blockstream.data.lightning.parseBolt11AndCheckExpired
import com.blockstream.data.lwk.Bolt12AmountMode
import com.blockstream.data.lwk.PaymentInstruction
import com.blockstream.data.swap.QuoteMode
import com.blockstream.data.swap.SwapAsset
import com.blockstream.data.utils.toAmountLook
import com.blockstream.domain.swap.InvoiceSwappability
import com.blockstream.domain.swap.SwapUseCase
import com.blockstream.jade.Loggable

class GetSendFlowUseCase(
    private val boltzUseCase: SwapUseCase,
    private val getSendAssetsUseCase: GetSendAssetsUseCase,
    private val getSendAccountsUseCase: GetSendAccountsUseCase,
    private val prepareTransactionUseCase: PrepareTransactionUseCase
) {

    suspend operator fun invoke(
        greenWallet: GreenWallet,
        session: GdkSession,
        address: String,
        asset: EnrichedAsset? = null,
        account: AccountAsset? = null,
    ): SendFlow {
        var asset = asset
        var account = account

        val isJadeCore = session.device?.isJadeCore?.value == true
        val swapsEnabled = boltzUseCase.isSwapsEnabledUseCase(wallet = greenWallet)

        if (looksLikeLightningInput(address) && (isJadeCore || !swapsEnabled)) {
            throw Exception("id_swaps_not_enabled_for_this_wallet")
        }

        val assets = getSendAssetsUseCase(session = session, address = address)

        val isLightningAddress = assets.any { it.isLightning }

        if (isLightningAddress && (isJadeCore || !swapsEnabled)) {
            throw Exception("id_swaps_not_enabled_for_this_wallet")
        }

        val instruction = if (isLightningAddress) {
            tryCatch { session.lwkOrNull?.inspectPaymentInstruction(address) }
        } else null

        if (instruction is PaymentInstruction.Bolt11) {
            val expired = tryCatch { parseBolt11AndCheckExpired(instruction.invoice) } == true
            if (expired) {
                throw Exception("id_invoice_expired")
            }
            if (instruction.amountSats == null && asset != null && !asset.isLightning) {
                throw Exception("id_no_amount_less_invoices_supported")
            }
        }

        when {
            assets.isEmpty() -> throw Exception("id_invalid_address")
            asset != null -> {
                // Check if can be a Swap, Liquid <> Lightning
                if (asset.isLiquidPolicyAsset(session) &&
                    assets.size == 1 &&
                    assets.first().isLightning &&
                    boltzUseCase.isInvoiceSwappableUseCase(address = address, session = session) is InvoiceSwappability.Swappable
                ) {
                    // Change asset to Lightning
                    asset = assets.first()
                } else {
                    assets.find {
                        it.assetId == asset.assetId
                    } ?: throw Exception("id_invalid_address")
                }
            }

            assets.size > 1 -> {
                return SendFlow.SelectAsset(address = address, assets = EnrichedAssetList(assets))
            }

            else -> {
                asset = assets.first()
            }
        }

        if (account == null) {
            val accounts = getSendAccountsUseCase(session = session, wallet = greenWallet, asset = asset, address = address)

            val invoiceAmount = (instruction as? PaymentInstruction.Bolt11)?.amountSats
                ?: (instruction as? PaymentInstruction.Bolt12)?.amountSats
            if (invoiceAmount != null && accounts.isNotEmpty() && accounts.maxOf { it.balance(session) } < invoiceAmount) {
                throw Exception("id_insufficient_funds_invoice_amount_s|${formatSatsForError(session, invoiceAmount)}")
            }

            if (invoiceAmount != null && isLightningAddress) {
                val quote = tryCatch {
                    session.lwkOrNull?.quote(
                        satoshi = invoiceAmount,
                        quoteMode = QuoteMode.SEND,
                        send = SwapAsset.Liquid,
                        receive = SwapAsset.Lightning,
                    )
                }
                if (quote != null && invoiceAmount > quote.maximal) {
                    throw Exception("id_amount_is_above_the_maximum_payment_limit_of_s|${formatSatsForError(session, quote.maximal)}")
                }
            }

            when {
                accounts.isEmpty() -> {
                    // Asset is Lightning but address is not swappable. Distinguish "amountless BOLT11"
                    // (recognised but unsupported) from generic invalid input so the user sees the
                    // actual reason instead of a catch-all "Invalid address". BOLT12 destinations
                    // are LBTC-only today, so when there's no eligible account surface that reason
                    // explicitly rather than the misleading "insufficient funds".
                    if (asset.isLightning) {
                        val instruction = tryCatch { session.lwkOrNull?.inspectPaymentInstruction(address) }
                        if (instruction is PaymentInstruction.Bolt12) {
                            throw Exception("id_bolt12_payment_is_only_available_via_lbtc")
                        }
                        when (boltzUseCase.isInvoiceSwappableUseCase(address = address, session = session)) {
                            is InvoiceSwappability.AmountlessBolt11 ->
                                throw Exception("id_no_amount_less_invoices_supported")
                            is InvoiceSwappability.Unknown ->
                                throw Exception("id_invalid_address")
                            is InvoiceSwappability.Swappable ->
                                throw Exception("id_insufficient_funds")
                        }
                    } else {
                        throw Exception("id_insufficient_funds")
                    }
                }
                accounts.size > 1 -> {
                    return SendFlow.SelectAccount(address = address, asset = asset, accounts = AccountAssetBalanceList(accounts))
                }

                else -> {
                    account = accounts.first().accountAsset
                }
            }
        }

        val isSwap =
            boltzUseCase.isLiquidToLightningSwapUseCase(
                wallet = greenWallet,
                asset = asset,
                address = address,
                accountAsset = account,
                session = session
            )

        if (isSwap) {
            val isAmountless = instruction is PaymentInstruction.LnUrl ||
                    (instruction is PaymentInstruction.Bolt12 && instruction.amountMode == Bolt12AmountMode.AMOUNTLESS)

            if (isAmountless) {
                return SendFlow.SelectLightningAmount(address = address, account = account)
            }

            val invoiceAmount = (instruction as? PaymentInstruction.Bolt11)?.amountSats
                ?: (instruction as? PaymentInstruction.Bolt12)?.amountSats
            if (invoiceAmount != null && account.balance(session) < invoiceAmount) {
                throw Exception("id_insufficient_funds_invoice_amount_s|${formatSatsForError(session, invoiceAmount)}")
            }

            if (invoiceAmount != null) {
                val quote = tryCatch {
                    session.lwkOrNull?.quote(
                        satoshi = invoiceAmount,
                        quoteMode = QuoteMode.SEND,
                        send = SwapAsset.Liquid,
                        receive = SwapAsset.Lightning,
                    )
                }
                if (quote != null && invoiceAmount > quote.maximal) {
                    throw Exception("id_amount_is_above_the_maximum_payment_limit_of_s|${formatSatsForError(session, quote.maximal)}")
                }
            }

            val params = prepareTransactionUseCase(
                greenWallet = greenWallet,
                session = session,
                accountAsset = account,
                address = address,
                paymentInstruction = instruction,
            )

            val tx = session.createTransaction(account.account.network, params)

            if (tx.error.isNullOrBlank()) {
                return SendFlow.SendConfirmation(
                    account = account,
                    params = params,
                    transaction = tx,
                )
            } else {
                throw Exception(tx.error)
            }
        }

        if (isLightningAddress && account.account.network.isLightning) {
            val lightningInput = tryCatch {
                session.lightningSdk.parseBoltOrLNUrlAndCache(address)
            }
            val bolt11Amount = (lightningInput as? LightningInputType.Bolt11)?.invoice?.amountSatoshi
                ?: (instruction as? PaymentInstruction.Bolt11)?.amountSats
            if (bolt11Amount != null) {
                val maxPayable = session.lightningSdkOrNull?.nodeInfoStateFlow?.value?.maxPayableSatoshi() ?: 0L
                if (maxPayable == 0L) {
                    throw Exception("id_lightning_balance_too_low_to_send")
                }
                if (bolt11Amount > maxPayable) {
                    throw Exception("id_amount_is_above_the_maximum_payment_limit_of_s|${formatSatsForError(session, maxPayable)}")
                }

                val params = prepareTransactionUseCase(
                    greenWallet = greenWallet,
                    session = session,
                    accountAsset = account,
                    address = address,
                    paymentInstruction = instruction,
                )

                val tx = session.createTransaction(account.account.network, params)

                if (tx.error.isNullOrBlank()) {
                    return SendFlow.SendLightningConfirmation(
                        account = account,
                        params = params,
                        transaction = tx,
                        invoice = address,
                    )
                } else {
                    throw Exception(tx.error)
                }
            }
        }

        return if (isLightningAddress) {
            SendFlow.SelectLightningAmount(address = address, account = account)
        } else {
            SendFlow.SelectAmount(address = address, account = account)
        }
    }

    private fun looksLikeLightningInput(input: String): Boolean {
        val trimmed = input.trim().removePrefix("lightning:").removePrefix("LIGHTNING:").lowercase()
        return trimmed.startsWith("lnbc") ||
                trimmed.startsWith("lntb") ||
                trimmed.startsWith("lnbcrt") ||
                trimmed.startsWith("lnsb") ||
                trimmed.startsWith("lnurl") ||
                trimmed.startsWith("lno1") ||
                ('@' in trimmed && !trimmed.contains(':'))
    }

    private suspend fun formatSatsForError(session: GdkSession, satoshi: Long): String {
        val sats = satoshi.toAmountLook(
            session = session,
            denomination = Denomination.SATOSHI,
            withUnit = true,
            withGrouping = true,
        ) ?: "$satoshi"
        val fiat = Denomination.fiat(session)?.let {
            satoshi.toAmountLook(
                session = session,
                denomination = it,
                withUnit = true,
                withGrouping = true,
            )
        }
        return if (fiat != null) "$sats ($fiat)" else sats
    }

    companion object : Loggable()
}
