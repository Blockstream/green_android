package com.blockstream.compose.models.assetaccounts

import com.blockstream.compose.utils.toStringHolder
import androidx.lifecycle.viewModelScope
import com.blockstream.data.data.DataState
import com.blockstream.data.data.Denomination
import com.blockstream.data.data.EnrichedAsset
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.extensions.filterForAsset
import com.blockstream.data.extensions.ifConnected
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.AccountAssetBalance
import com.blockstream.data.utils.toAmountLook
import com.blockstream.compose.events.Event
import com.blockstream.compose.looks.transaction.TransactionLook
import com.blockstream.compose.models.overview.WalletBalanceViewModel
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.sideeffects.SideEffects
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class AssetAccountListViewModelAbstract(
    greenWallet: GreenWallet, val assetId: String
) : WalletBalanceViewModel(greenWallet = greenWallet) {
    override fun screenName(): String = "AssetAccountList"
    abstract val accounts: StateFlow<List<AccountAssetBalance>>
    abstract val isLoading: StateFlow<Boolean>
    abstract val asset: StateFlow<EnrichedAsset?>
    abstract val totalBalance: StateFlow<String>
    abstract val totalBalanceFiat: StateFlow<String?>
}

class AssetAccountListViewModel(
    greenWallet: GreenWallet, assetId: String
) : AssetAccountListViewModelAbstract(greenWallet = greenWallet, assetId = assetId) {

    class LocalEvents {
        class AccountClick(val accountAssetBalance: AccountAssetBalance) : Event
    }

    final override val accounts : StateFlow<List<AccountAssetBalance>>
        field = MutableStateFlow<List<AccountAssetBalance>>(emptyList())

    final override val isLoading : StateFlow<Boolean>
        field = MutableStateFlow(true)

    private val _asset = MutableStateFlow<EnrichedAsset?>(null)
    override val asset: StateFlow<EnrichedAsset?> = _asset

    private val _totalBalance = MutableStateFlow("")
    override val totalBalance: StateFlow<String> = _totalBalance

    private val _totalBalanceFiat = MutableStateFlow<String?>(null)
    override val totalBalanceFiat: StateFlow<String?> = _totalBalanceFiat

    init {
        viewModelScope.launch {
            val asset = EnrichedAsset.create(session, assetId)

            _asset.value = asset
            _navData.value = NavData(
                title = asset.name(session).toStringHolder(),
            )
        }

        session.ifConnected {
            combine(
                session.accounts,
                merge(flowOf(Unit), session.accountsAndBalanceUpdated), // set initial value
                hideAmounts
            ) { accounts, _, _ ->
                accounts.filterForAsset(assetId, session).map { account ->
                    AccountAssetBalance.create(
                        accountAsset = AccountAsset.fromAccountAsset(
                            account = account, assetId = assetId, session = session
                        ), session = session
                    )
                }
            }.onEach { accountsList ->
                accounts.value = accountsList
                updateTotalBalance(accountsList)
                isLoading.value = false
            }.launchIn(viewModelScope)
        }

        bootstrap()
    }

    private fun updateTotalBalance(accountBalances: List<AccountAssetBalance>) {
        viewModelScope.launch {
            val isHidden = hideAmounts.value
            var totalSatoshi = 0L
            accountBalances.forEach { accountAssetBalance ->
                val balance = accountAssetBalance.accountAsset.balance(session)
                totalSatoshi += balance
            }

            _totalBalance.value = if (isHidden) "*****" else totalSatoshi.toAmountLook(
                session = session, assetId = assetId, withUnit = true, withGrouping = true, withMinimumDigits = false
            ) ?: "0"

            _totalBalanceFiat.value = if (isHidden) "*****" else if (totalSatoshi > 0) {
                totalSatoshi.toAmountLook(
                    session = session,
                    assetId = assetId,
                    withUnit = true,
                    denomination = Denomination.fiat(session)
                )
            } else null
        }
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.AccountClick -> {
                postSideEffect(
                    SideEffects.NavigateTo(
                        NavigateDestinations.AssetAccountDetails(
                            accountAsset = event.accountAssetBalance.accountAsset, greenWallet = greenWallet
                        )
                    )
                )
            }
        }
    }
}

class AssetAccountListViewModelPreview(
    greenWallet: GreenWallet, assetId: String
) : AssetAccountListViewModelAbstract(greenWallet = greenWallet, assetId = assetId) {

    override val accounts: StateFlow<List<AccountAssetBalance>> = MutableStateFlow(emptyList())
    override val isLoading: StateFlow<Boolean> = MutableStateFlow(false)
    override val asset: StateFlow<EnrichedAsset?> = MutableStateFlow(EnrichedAsset.PreviewBTC)
    override val totalBalance: StateFlow<String> = MutableStateFlow("0.0002821 BTC")
    override val totalBalanceFiat: StateFlow<String?> = MutableStateFlow("US$ 2,321.00")

    companion object {
        fun preview() = AssetAccountListViewModelPreview(
            greenWallet = previewWallet(), assetId = "btc"
        )
    }
}