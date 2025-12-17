@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.blockstream.compose.models.swap

import androidx.lifecycle.viewModelScope
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_swap
import com.blockstream.compose.extensions.launchIn
import com.blockstream.compose.extensions.previewAccountAssetBalance
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
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.extensions.ifConnected
import com.blockstream.data.extensions.isNotBlank
import com.blockstream.data.extensions.launchSafe
import com.blockstream.data.extensions.tryCatch
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.AccountAssetBalance
import com.blockstream.data.gdk.data.AccountAssetBalanceList
import com.blockstream.data.gdk.data.PendingTransaction
import com.blockstream.data.gdk.params.CreateTransactionParams
import com.blockstream.data.swap.Quote
import com.blockstream.data.swap.QuoteMode
import com.blockstream.data.utils.UserInput
import com.blockstream.data.utils.feeRateWithUnit
import com.blockstream.data.utils.ifNotNull
import com.blockstream.domain.swap.SwapUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
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
    val from: AccountAssetBalance? = null,
    val to: AccountAssetBalance? = null,
    val fromAccounts: List<AccountAssetBalance> = emptyList(),
    val toAccounts: List<AccountAssetBalance> = emptyList(),
    val quoteMode: QuoteMode = QuoteMode.SEND,
    val amountFrom: String = "",
    val amountFromExchange: String = "",
    val amountTo: String = "",
    val amountToExchange: String = "",
    val error: String? = null,
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

    abstract fun onAccountClick(isFrom: Boolean)
    abstract fun setAccount(accountAssetBalance: AccountAssetBalance)
}

class SwapViewModel(
    greenWallet: GreenWallet,
    val accountAssetOrNull: AccountAsset? = null,
) : SwapViewModelAbstract(
    greenWallet = greenWallet,
    accountAssetOrNull = accountAssetOrNull
) {
    private val swapUseCase: SwapUseCase by inject()

    override val uiState: MutableStateFlow<SwapUiState> = MutableStateFlow(SwapUiState())

    private var quote: Quote? = null

    private var _pendingSetAccountFrom = true

    init {
        viewModelScope.launch {
            _navData.value = NavData(title = getString(Res.string.id_swap), subtitle = greenWallet.name)
        }

        viewModelScope.ifConnected(session) {
            // Update From Accounts
            uiState.update { uiState ->
                val fromAccounts = session.accountAsset.value
                    .filter { it.asset.isPolicyAsset(session) } // Only policy assets for now

                val accountsWithBalance = fromAccounts.mapNotNull {
                    AccountAssetBalance.createIfBalance(
                        accountAsset = it,
                        session = sessionOrNull
                    )
                }

                val from = accountAssetOrNull
                    ?.let { AccountAssetBalance.create(accountAsset = it, session = session) }
                    ?: accountsWithBalance.firstOrNull()
                    ?: fromAccounts.firstOrNull()?.let { AccountAssetBalance.create(accountAsset = it, session = session) }

                uiState.copy(
                    from = from,
                    fromAccounts = accountsWithBalance
                )
            }

            session.accountAsset.value
                .filter { it.asset.isPolicyAsset(session) } // Only policy assets for now
                .mapNotNull {
                    AccountAssetBalance.createIfBalance(
                        accountAsset = it,
                        session = sessionOrNull
                    )
                }
        }

        uiState.map { it.from }.filterNotNull().distinctUntilChanged().onEach { from ->
            accountAsset.value = from.accountAsset
            _network.value = from.account.network

            swapUseCase.getSwappableAccountsUseCase(session = session, swapFrom = from).also { accounts ->
                if (accounts.find { it.accountAsset == from.accountAsset } == null) {
                    uiState.update { uiState ->
                        uiState.copy(
                            to = accounts.firstOrNull()
                        )
                    }
                }
            }

            // Update To Accounts
            swapUseCase.getSwappableAccountsUseCase(session = session, swapFrom = uiState.value.from).also { toAccounts ->
                uiState.update { uiState ->
                    uiState.copy(
                        to = toAccounts.find { it.accountAsset == uiState.to?.accountAsset } ?: toAccounts.firstOrNull(),
                        toAccounts = toAccounts
                    )
                }
            }

        }.launchIn(this)

        uiState.map { it.error }.distinctUntilChanged().onEach { error ->
            _isValid.value = error == null
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
                    error = it.error
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
            accountAsset.value = uiState.value.from?.accountAsset
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
        }.launchIn(this)

        bootstrap()
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
            address = session.getReceiveAddress(accountAsset.value!!.account).address,
            amount = uiState.value.amountFrom,
            denomination = denomination.value,
            feeRate = getFeeRate()
        )
    }

    override fun createTransaction(params: CreateTransactionParams?, finalCheckBeforeContinue: Boolean) {
        viewModelScope.launchSafe {
            val account = uiState.value.from?.account
            if (params != null && account != null) {
                val tx = session.createTransaction(account.network, params)

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
        postSideEffect(
            SideEffects.NavigateTo(
                NavigateDestinations.Accounts(
                    greenWallet = greenWallet,
                    accounts = AccountAssetBalanceList(
                        (if (isFrom) uiState.value.fromAccounts else uiState.value.toAccounts)
                    ),
                    withAsset = isFrom,
                    withArrow = false
                )
            )
        )
    }

    override fun setAccount(accountAssetBalance: AccountAssetBalance) {
        uiState.update { uiState ->
            if (_pendingSetAccountFrom) {
                uiState.copy(
                    from = accountAssetBalance
                )
            } else {
                uiState.copy(
                    to = accountAssetBalance
                )
            }
        }
    }

    override fun createSwap() {
        doAsync({

            val from = checkNotNull(uiState.value.from?.accountAsset)
            val to = checkNotNull(uiState.value.to?.accountAsset)

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

            val tx = session.createTransaction(from.account.network, params)

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
                NavigateDestinations.SendConfirm(
                    greenWallet = greenWallet,
                    accountAsset = from,
                    denomination = denomination.value
                )
            )

        }, onSuccess = {
            postSideEffect(it)
        })

    }

    override suspend fun denominatedValue(): DenominatedValue? {
        val (accountAsset, amount) = uiState.value.let { if (it.quoteMode.isSend) it.from?.accountAsset to it.amountFrom else it.to?.accountAsset to it.amountTo }

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

    override fun setDenominatedValue(denominatedValue: DenominatedValue) {
        _denomination.value = denominatedValue.denomination
        uiState.update { uiState ->
            uiState.copy(
                amountFrom = denominatedValue.asInput ?: ""
            )
        }
    }

    override fun swapPairs() {
        uiState.update { uiState ->
            uiState.copy(
                from = uiState.to,
                to = uiState.from
            ).also {
                ifNotNull(it.from, it.to) { from, to ->
                    countly.swapToggle(session = session, from = from.accountAsset, to = to.accountAsset)
                }
            }
        }
    }
}

class SwapViewModelPreview(greenWallet: GreenWallet) :
    SwapViewModelAbstract(greenWallet = greenWallet) {

    override val uiState: MutableStateFlow<SwapUiState> =
        MutableStateFlow(SwapUiState(from = previewAccountAssetBalance(), to = previewAccountAssetBalance()))

    override fun createSwap() {}
    override fun onAmountChanged(amount: String, isFromInput: Boolean) {}

    override fun onAccountClick(isFrom: Boolean) {}

    override fun setAccount(accountAssetBalance: AccountAssetBalance) {}

    override fun swapPairs() {}

    init {
        banner.value = Banner.preview3
        _showFeeSelector.value = true
    }

    companion object {
        fun preview() = SwapViewModelPreview(previewWallet())
    }
}