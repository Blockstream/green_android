@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.blockstream.compose.models.swap

import com.blockstream.compose.utils.toStringHolder
import com.blockstream.compose.utils.StringHolder
import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_account_selector
import blockstream_green.common.generated.resources.id_swap
import blockstream_green.common.generated.resources.id_swap_from
import blockstream_green.common.generated.resources.id_swap_to
import blockstream_green.common.generated.resources.id_no_stuck_swaps_to_reset
import blockstream_green.common.generated.resources.id_swaps_queued_for_retry
import blockstream_green.common.generated.resources.id_swaps_will_be_reprocessed_when_the_app
import com.blockstream.compose.events.Events
import com.blockstream.compose.extensions.launchIn
import com.blockstream.compose.extensions.previewAccountAsset
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.models.send.CreateTransactionViewModelAbstract
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.AddressInputType
import com.blockstream.data.TransactionSegmentation
import com.blockstream.data.TransactionType
import com.blockstream.data.banner.Banner
import com.blockstream.data.data.DenominatedValue
import com.blockstream.data.data.Denomination
import com.blockstream.data.data.FeePriority
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.extensions.ifConnected
import com.blockstream.data.extensions.isNotBlank
import com.blockstream.data.extensions.launchSafe
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.AccountAssetBalance
import com.blockstream.data.gdk.data.AccountAssetBalanceList
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.AssetBalance
import com.blockstream.data.gdk.data.AssetBalanceList
import com.blockstream.data.gdk.data.PendingTransaction
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.lightning.maxSendableSatoshi
import com.blockstream.data.swap.Quote
import com.blockstream.data.swap.QuoteMode
import com.blockstream.data.swap.SwapErrorSide
import com.blockstream.data.utils.UserInput
import com.blockstream.data.utils.feeRateWithUnit
import com.blockstream.data.utils.toAmountLook
import com.blockstream.domain.receive.GetReceiveAddressUseCase
import com.blockstream.domain.swap.HasWalletSwapsUseCase
import com.blockstream.domain.swap.ResetSwapsResult
import com.blockstream.domain.swap.ResetWalletSwapsUseCase
import com.blockstream.domain.swap.SwapUseCase
import com.blockstream.domain.swap.isSwappableAsset
import com.blockstream.jade.Loggable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

data class SwapUiState(
    val from: AccountAsset? = null,
    val fromBalance: String? = null,
    val to: AccountAsset? = null,
    val toBalance: String? = null,
    val fromAccounts: List<AccountAssetBalance> = emptyList(),
    val toAccounts: List<AccountAssetBalance> = emptyList(),
    val quoteMode: QuoteMode = QuoteMode.SEND,
    val amountFrom: String = "",
    val amountFromExchange: String = "",
    val amountTo: String = "",
    val amountToExchange: String = "",
    val error: String? = null,
    val errorSide: SwapErrorSide = SwapErrorSide.NONE,
    val isValidQuote: Boolean = false,
    val isSwapCreationAvailable: Boolean = true,
    val hasSwaps: Boolean = false
)

abstract class SwapViewModelAbstract(
    greenWallet: GreenWallet,
    accountAssetOrNull: AccountAsset? = null
) : CreateTransactionViewModelAbstract(
    greenWallet = greenWallet,
    accountAssetOrNull = accountAssetOrNull
) {
    override fun screenName(): String = "Swap"

    override fun segmentation(): HashMap<String, Any>? {
        return countly.sessionSegmentation(session = session)
    }

    abstract val uiState: MutableStateFlow<SwapUiState>

    abstract fun swapPairs()
    abstract fun createSwap()
    abstract fun onAmountChanged(amount: String, isSendQuoteMode: Boolean)
    abstract fun onQuoteModeChanged(isSendQuoteMode: Boolean)
    abstract fun onAccountClick(isFrom: Boolean)
    abstract fun setAccount(accountAssetBalance: AccountAssetBalance)
    abstract fun onAssetClick(isFrom: Boolean)
    abstract fun setAsset(assetBalance: AssetBalance)
    abstract fun resetSwaps()

    abstract fun isDirectionAvailable(from: AccountAsset, to: AccountAsset): Boolean
}

class SwapViewModel(
    greenWallet: GreenWallet,
    val accountAssetOrNull: AccountAsset? = null,
) : SwapViewModelAbstract(
    greenWallet = greenWallet,
    accountAssetOrNull = accountAssetOrNull
) {
    private val swapUseCase: SwapUseCase by inject()
    private val getReceiveAddressUseCase: GetReceiveAddressUseCase by inject()
    private val resetWalletSwapsUseCase: ResetWalletSwapsUseCase by inject()
    private val hasWalletSwapsUseCase: HasWalletSwapsUseCase by inject()

    private val isSwapCreationAvailable: Boolean =
        swapUseCase.isSwapDirectionAvailableUseCase.hasAnyEnabledDirection()

    override val uiState: MutableStateFlow<SwapUiState> =
        MutableStateFlow(SwapUiState(isSwapCreationAvailable = isSwapCreationAvailable))

    private var quote: Quote? = null

    private var _pendingSetAccountFrom = true

    private var resetJob: Job? = null

    init {
        _navData.value = NavData(
            title = StringHolder(stringResource = Res.string.id_swap),
            subtitle = greenWallet.name.toStringHolder()
        )

        viewModelScope.launch {
            val hasSwaps = runCatching { hasWalletSwapsUseCase(greenWallet.xPubHashId) }
                .onFailure { logger.e(it) { "hasWalletSwaps failed" } }
                .getOrDefault(true)

            uiState.update { it.copy(hasSwaps = hasSwaps) }
        }

        if (isSwapCreationAvailable) {
            observeSwapForm()
        }

        bootstrap()
    }

    private fun observeSwapForm() {
        viewModelScope.ifConnected(session) {
            // Update From Accounts
            uiState.update { uiState ->
                val swappable = swappableAccounts()

                val accountsWithBalance = swappable.filter {
                    it.balance(session) > 0
                }

                val from = accountAssetOrNull
                    ?: accountsWithBalance.firstOrNull()
                    ?: swappable.firstOrNull()

                uiState.copy(
                    from = from
                )
            }
        }

        // From row: balance, account selector list and destination default
        combine(uiState.map { it.from }.filterNotNull().distinctUntilChanged(), denomination) { from, denomination ->

            uiState.update { uiState ->
                uiState.copy(
                    fromBalance = swapBalance(from).toAmountLook(
                        session = session,
                        assetId = from.assetId,
                        withUnit = true,
                        denomination = denomination
                    ),
                    fromAccounts = accountsFor(row = from, denomination = denomination)
                )
            }

            // Keep the current destination unless it now collides with the source asset;
            // otherwise fall back to the per-source default (Bitcoin -> L-BTC, else -> Bitcoin).
            uiState.update { uiState ->
                uiState.copy(
                    to = uiState.to?.takeIf { !it.account.network.isSameNetwork(from.account.network) }
                        ?: defaultTo(from = from, accounts = swappableAccounts())
                )
            }
        }.launchIn(this)

        // To row: balance and account selector list
        combine(uiState.map { it.to }.filterNotNull().distinctUntilChanged(), denomination) { to, denomination ->
            uiState.update { uiState ->
                uiState.copy(
                    toBalance = swapBalance(to).toAmountLook(
                        session = session,
                        assetId = to.assetId,
                        withUnit = true,
                        denomination = denomination
                    ),
                    toAccounts = accountsFor(row = to, denomination = denomination)
                )
            }
        }.launchIn(this)

        uiState.map { it.from }.filterNotNull().distinctUntilChanged().onEach { from ->
            accountAsset.value = from
            _network.value = from.account.network
        }.launchIn(this)

        uiState.map { it.isValidQuote }.distinctUntilChanged().onEach {
            _isValid.value = it
        }.launchIn(this)

        uiState.map { it.error }.distinctUntilChanged().onEach { error ->
            _error.value = error
        }.launchIn(this)

        swapUseCase.getSwapAmountUseCase(
            session = session,
            from = uiState.map { it.from }.filterNotNull(),
            to = uiState.map { it.to }.filterNotNull(),
            amountFrom = uiState.map { it.amountFrom },
            amountTo = uiState.map { it.amountTo },
            quoteMode = uiState.map { it.quoteMode },
            denomination = denomination
        ).onEach {
            quote = it.quote

            uiState.update { uiState ->
                val uiState = uiState.copy(
                    amountFromExchange = it.amountFromExchange ?: "",
                    amountToExchange = it.amountToExchange ?: "",
                    error = it.error,
                    errorSide = it.errorSide,
                    isValidQuote = it.isValid
                )

                if (uiState.quoteMode.isSend) {
                    uiState.copy(
                        amountTo = it.amountTo,
                    )
                } else {
                    uiState.copy(
                        amountFrom = it.amountFrom,
                    )
                }
            }
        }.launchIn(this)

        // Sync if it changes from anywhere else
        // This should not trigger a change if the account is the same
        accountAsset.onEach {
            accountAsset.value = uiState.value.from
        }.launchIn(this)

        combine(
            _network.filterNotNull(),
            _feeEstimation,
            uiState.map { it.amountFrom }.distinctUntilChanged(),
            _feePriorityPrimitive, merge(flowOf(Unit), session.accountsAndBalanceUpdated)
        ) {
            createTransactionParams.value = tryCatch(context = Dispatchers.Default) { createTransactionParams() }
        }.launchIn(this)

        _network.onEach {
            _showFeeSelector.value = sendUseCase.showFeeSelectorUseCase(session = session, network = it)
            // Reset fee priority, this is important as can be changed by the user and persisted in liquid
            _feePriority.value = FeePriority.Low()
        }.launchIn(this)
    }

    override fun onQuoteModeChanged(isSendQuoteMode: Boolean) {
        uiState.update {
            if (isSendQuoteMode) {
                it.copy(quoteMode = QuoteMode.SEND)
            } else {
                it.copy(quoteMode = QuoteMode.RECEIVE)
            }
        }
    }

    override fun onAmountChanged(amount: String, isSendQuoteMode: Boolean) {
        uiState.update {
            if (isSendQuoteMode) {
                it.copy(amountFrom = amount, quoteMode = QuoteMode.SEND)
            } else {
                it.copy(amountTo = amount, quoteMode = QuoteMode.RECEIVE)
            }
        }
    }

    override suspend fun createTransactionParams(): CreateTransactionParams {
        return sendUseCase.prepareTransactionUseCase(
            greenWallet = greenWallet,
            session = session,
            accountAsset = accountAsset.value!!,
            address = getReceiveAddressUseCase(session, accountAsset.value!!.account).address,
            amount = uiState.value.amountFrom,
            denomination = denomination.value,
            feeRate = getFeeRate()
        )
    }

    override fun createTransaction(params: CreateTransactionParams?, finalCheckBeforeContinue: Boolean) {
        viewModelScope.launchSafe {
            val account = uiState.value.from?.account
            if (params != null && account != null) {
                val tx = session.createTransaction(account = account, params = params)

                tx.fee?.takeIf { it != 0L || tx.error.isNullOrBlank() }.also {
                    _feePriority.value = calculateFeePriority(
                        session = session,
                        feePriority = _feePriority.value,
                        feeAmount = it,
                        feeRate = tx.feeRate?.feeRateWithUnit()
                    )
                }
            }
        }
    }

    override fun onAccountClick(isFrom: Boolean) {
        _pendingSetAccountFrom = isFrom
        doAsync({
            // Build the list from the row's current value to avoid showing a stale
            // (pre asset-change) account list from uiState.
            val row = checkNotNull(if (isFrom) uiState.value.from else uiState.value.to)
            NavigateDestinations.Accounts(
                greenWallet = greenWallet,
                accounts = AccountAssetBalanceList(accountsFor(row = row, denomination = denomination.value)),
                title = getString(Res.string.id_account_selector),
                withAsset = false,
                withArrow = false
            )
        }, onSuccess = {
            postSideEffect(SideEffects.NavigateTo(it))
        })
    }

    override fun onAssetClick(isFrom: Boolean) {
        _pendingSetAccountFrom = isFrom
        doAsync({
            // Show every swappable asset. Picking an unsupported pair (eg. Liquid -> Lightning) is
            // allowed here and surfaces the "not supported yet" error rather than being hidden, and
            // picking the opposite row's asset flips the rows via setAccount's collision handling.
            val entries = swappableAccounts().distinctBy { it.account.network.canonicalNetworkId }
            NavigateDestinations.SwapAssets(
                greenWallet = greenWallet,
                assets = AssetBalanceList(entries.map { AssetBalance.create(it.asset) }),
                title = pickerTitle(isFrom)
            )
        }, onSuccess = {
            postSideEffect(SideEffects.NavigateTo(it))
        })
    }

    // Selecting an asset picks its default account (standard singlesig, or the single Lightning
    // account); the row-flip collision handling in setAccount applies as usual.
    override fun setAsset(assetBalance: AssetBalance) {
        val current = if (_pendingSetAccountFrom) uiState.value.from else uiState.value.to
        // Re-selecting the row's current asset keeps the user's account choice.
        if (current?.assetId == assetBalance.assetId) return

        swappableAccounts()
            .filter { it.assetId == assetBalance.assetId }
            .let { accounts ->
                // The source must be fundable, so prefer a balance-bearing account for the From
                // row; otherwise the standard singlesig account, falling back to the first one.
                accounts.takeIf { _pendingSetAccountFrom }?.firstOrNull { it.balance(session) > 0 }
                    ?: accounts.firstOrNull { it.account.type == AccountType.BIP84_SEGWIT }
                    ?: accounts.firstOrNull()
            }
            ?.also { setAccount(AccountAssetBalance.create(it)) }
    }

    override fun setAccount(accountAssetBalance: AccountAssetBalance) {
        val selected = accountAssetBalance.accountAsset
        uiState.update { uiState ->
            if (_pendingSetAccountFrom) {
                // The two rows can never reference the same asset: if the new source collides with the
                // destination, move the old source into the destination row so the user's pick is kept.
                val collision = uiState.to?.account?.network?.isSameNetwork(selected.account.network) == true
                if (collision) uiState.copy(from = selected, to = uiState.from) else uiState.copy(from = selected)
            } else {
                val collision = uiState.from?.account?.network?.isSameNetwork(selected.account.network) == true
                if (collision) uiState.copy(to = selected, from = uiState.to) else uiState.copy(to = selected)
            }
        }
    }

    override fun createSwap() {
        val from = checkNotNull(uiState.value.from)
        val to = checkNotNull(uiState.value.to)

        postEvent(Events.SwapInitiate(from = from.network, to = to.network))

        doAsync({
            val params = swapUseCase.prepareSwapTransactionUseCase(
                greenWallet = greenWallet,
                session = session,
                from = from,
                to = to,
                amount = uiState.value.amountFrom,
                denomination = denomination.value,
                quote = quote,
                feeRate = getFeeRate()
            )

            val tx = session.createTransaction(account = from.account, params = params)

            if (tx.error.isNotBlank()) {
                throw Exception(tx.error)
            }

            session.pendingTransaction = PendingTransaction(
                params = params,
                transaction = tx,
                segmentation = TransactionSegmentation(
                    transactionType = TransactionType.SWAP,
                    addressInputType = AddressInputType.INTERNAL
                )
            )

            SideEffects.NavigateTo(
                if (from.account.isLightning) {
                    // Lightning source: the reverse-swap invoice is paid from the Lightning balance
                    // via sendLightningTransaction, so route to the Lightning confirm screen.
                    NavigateDestinations.SendLightningConfirm(
                        greenWallet = greenWallet,
                        accountAsset = from,
                        invoice = params.swap?.address ?: "",
                        // The Lightning balance is debited for the invoice amount (fromAmount), not
                        // the BTC the user receives (toAmount).
                        amountSatoshi = params.swap?.fromAmount,
                        denomination = denomination.value
                    )
                } else {
                    NavigateDestinations.SendConfirm(
                        greenWallet = greenWallet,
                        accountAsset = from,
                        denomination = denomination.value
                    )
                }
            )

        }, onSuccess = {
            postSideEffect(it)
        })
    }

    override suspend fun denominatedValue(): DenominatedValue? {
        val (accountAsset, amount) = uiState.value.let { if (it.quoteMode.isSend) it.from to it.amountFrom else it.to to it.amountTo }

        return accountAsset?.let { accountAsset ->
            UserInput.parseUserInputSafe(
                session = session,
                input = amount,
                denomination = denomination.value,
                assetId = accountAsset.assetId
            ).getBalance().let {
                DenominatedValue(
                    balance = it,
                    assetId = accountAsset.assetId,
                    denomination = denomination.value
                )
            }
        }
    }

    override suspend fun setDenominatedValue(denominatedValue: DenominatedValue) {
        _denomination.value = denominatedValue.denomination
        uiState.update { uiState ->
            if (uiState.quoteMode.isSend) {
                uiState.copy(
                    amountFrom = denominatedValue.asInput ?: ""
                )
            } else {
                uiState.copy(
                    amountTo = denominatedValue.asInput ?: ""
                )
            }
        }
    }

    override fun swapPairs() {
        uiState.update { uiState ->
            uiState.copy(
                from = uiState.to,
                to = uiState.from
            )
        }
    }

    // Available balance shown per row: the spendable channel balance for Lightning, the account
    // balance otherwise.
    private fun swapBalance(accountAsset: AccountAsset): Long =
        if (accountAsset.account.isLightning) {
            // Fee-adjusted so the shown "Available" is an amount the user can actually swap.
            session.lightningSdkOrNull?.nodeInfoStateFlow?.value?.maxSendableSatoshi() ?: 0L
        } else {
            accountAsset.balance(session)
        }

    // Default destination for a given source asset: Bitcoin -> Liquid Bitcoin, otherwise -> Bitcoin
    // (Lightning -> Bitcoin, Liquid -> Bitcoin).
    private fun defaultTo(from: AccountAsset, accounts: List<AccountAsset>): AccountAsset? =
        (if (from.account.isBitcoin) {
            accounts.firstOrNull { it.account.isLiquid }
        } else {
            accounts.firstOrNull { it.account.isBitcoin }
        }) ?: accounts.firstOrNull { !it.account.network.isSameNetwork(from.account.network) }

    private fun swappableAccounts(): List<AccountAsset> =
        session.accountAsset.value.filter { it.isSwappableAsset(session) }

    // Account selector entries for a row: only the accounts of the row's selected asset.
    private suspend fun accountsFor(row: AccountAsset, denomination: Denomination): List<AccountAssetBalance> =
        swappableAccounts()
            .filter { it.account.network.isSameNetwork(row.account.network) }
            .map {
                AccountAssetBalance.create(
                    accountAsset = it,
                    session = sessionOrNull,
                    isMaxPayable = it.account.isLightning,
                    denomination = denomination
                )
            }

    private suspend fun pickerTitle(isFrom: Boolean): String =
        getString(if (isFrom) Res.string.id_swap_from else Res.string.id_swap_to)

    override fun isDirectionAvailable(from: AccountAsset, to: AccountAsset): Boolean =
        swapUseCase.isSwapDirectionAvailableUseCase(from, to).isAvailable

    override fun resetSwaps() {
        if (resetJob?.isActive == true) return

        resetJob = doAsync({
            resetWalletSwapsUseCase(session = session, xPubHashId = greenWallet.xPubHashId)
        }, mutex = getMutex("resetSwaps"), onSuccess = { result ->
            when (result) {
                is ResetSwapsResult.NoSwaps -> {
                    uiState.update { it.copy(hasSwaps = false) }
                    postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_no_stuck_swaps_to_reset)))
                }

                is ResetSwapsResult.Queued ->
                    postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_swaps_will_be_reprocessed_when_the_app)))

                is ResetSwapsResult.Reprocessing -> {
                    postSideEffect(SideEffects.Snackbar(StringHolder.create(Res.string.id_swaps_queued_for_retry)))
                    postSideEffect(SideEffects.NavigateBack())
                }
            }
        })
    }

    companion object : Loggable()
}

class SwapViewModelPreview(
    greenWallet: GreenWallet,
    isSwapCreationAvailable: Boolean = true,
    hasSwaps: Boolean = false
) : SwapViewModelAbstract(greenWallet = greenWallet) {
    override val uiState: MutableStateFlow<SwapUiState> =
        MutableStateFlow(
            SwapUiState(
                from = previewAccountAsset(),
                to = previewAccountAsset(),
                isSwapCreationAvailable = isSwapCreationAvailable,
                hasSwaps = hasSwaps
            )
        )

    override fun createSwap() {}
    override fun onAmountChanged(amount: String, isFromInput: Boolean) {}
    override fun onQuoteModeChanged(isSendQuoteMode: Boolean) {}

    override fun onAccountClick(isFrom: Boolean) {}

    override fun setAccount(accountAssetBalance: AccountAssetBalance) {}
    override fun resetSwaps() {}

    override fun onAssetClick(isFrom: Boolean) {}

    override fun setAsset(assetBalance: AssetBalance) {}

    override fun swapPairs() {}

    override fun isDirectionAvailable(from: AccountAsset, to: AccountAsset): Boolean = true

    init {
        banner.value = Banner.preview3
        _showFeeSelector.value = true
    }

    companion object {
        fun preview() = SwapViewModelPreview(previewWallet())

        fun previewUnavailable(hasSwaps: Boolean = true) =
            SwapViewModelPreview(previewWallet(), isSwapCreationAvailable = false, hasSwaps = hasSwaps)
    }
}
