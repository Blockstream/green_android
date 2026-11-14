package com.blockstream.compose.models.send

import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.funnel_outline_active
import blockstream_green.common.generated.resources.id_coin_selection
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Funnel
import com.blockstream.compose.events.Event
import com.blockstream.compose.extensions.previewAccountAsset
import com.blockstream.compose.extensions.previewWallet
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavAction
import com.blockstream.compose.navigation.NavData
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.setResult
import com.blockstream.compose.sideeffects.SideEffects
import com.blockstream.data.data.Denomination
import com.blockstream.data.data.GreenWallet
import com.blockstream.data.gdk.data.AccountAsset
import com.blockstream.data.gdk.data.AccountType
import com.blockstream.data.gdk.data.shortOutpoint
import com.blockstream.data.utils.toAmountLook
import com.blockstream.domain.send.SendUseCase
import com.blockstream.domain.send.SpendableUtxo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import org.jetbrains.compose.resources.getString
import org.koin.core.component.inject

@Serializable
enum class CoinFilter {
    DUST,
    EXPIRED,
    LEGACY_RECOVERY
}

@Serializable
enum class CoinSort {
    AMOUNT_HIGH_TO_LOW,
    AMOUNT_LOW_TO_HIGH,
    NEWEST,
    OLDEST
}

data class CoinSelectionListItem(
    val id: String,
    val amount: String,
    val amountFiat: String? = null,
    val satoshi: Long,
    val txHash: String,
    val outputIndex: Long,
    val outpoint: String,
    val addressType: String,
    val blockHeight: Long?,
    val expiryHeight: Long?,
    val isBlinded: Boolean?,
    val isConfirmed: Boolean,
    val isDust: Boolean = false,
    val isExpired: Boolean = false,
    val isLegacyRecovery: Boolean = false,
    val isSelected: Boolean = false
)

data class CoinSelectionSummary(
    val count: Int = 0,
    val amount: String? = null,
    val amountFiat: String? = null,
    val canConfirm: Boolean = false
)

data class CoinSelectionResult(
    val selectedUtxoIds: List<String>,
    val selectedAmountSatoshi: Long? = null,
    val gdkPayloadUtxos: Map<String, List<JsonElement>>
)

@Serializable
data class CoinFilterResult(
    val filters: Set<CoinFilter>,
    val sort: CoinSort
)

private data class CoinSelectionData(
    val spendableUtxos: List<SpendableUtxo>,
    val items: List<CoinSelectionListItem>
)

abstract class CoinSelectionViewModelAbstract(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset
) : GreenViewModel(greenWalletOrNull = greenWallet, accountAssetOrNull = accountAsset) {
    class LocalEvents {
        data class ToggleCoin(val id: String) : Event
        data class ApplyFilters(val result: CoinFilterResult) : Event
        data class SelectSort(val sort: CoinSort) : Event
        data class OpenCoinInfo(val coin: CoinSelectionListItem) : Event
        object ToggleVisibleCoinsSelection : Event
        object ConfirmSelection : Event
        object OpenFilters : Event
        object OpenSort : Event
        object Refresh : Event
    }

    override fun screenName(): String = "CoinSelection"

    abstract val coins: StateFlow<List<CoinSelectionListItem>>
    abstract val coinsCount: StateFlow<Int>
    abstract val summary: StateFlow<CoinSelectionSummary>
    abstract val allVisibleCoinsSelected: StateFlow<Boolean>
    abstract val selectedFilters: StateFlow<Set<CoinFilter>>
    abstract val availableFilters: StateFlow<List<CoinFilter>>
    abstract val selectedSort: StateFlow<CoinSort>
    abstract val hasError: StateFlow<Boolean>
}

class CoinSelectionViewModel(
    greenWallet: GreenWallet,
    private val selectedAccountAsset: AccountAsset,
    private val displayDenomination: Denomination? = null,
    private val selectedUtxoIds: List<String> = emptyList()
) : CoinSelectionViewModelAbstract(greenWallet = greenWallet, accountAsset = selectedAccountAsset) {
    private val sendUseCase: SendUseCase by inject()

    private var spendableUtxos: List<SpendableUtxo> = emptyList()
    private var allCoins: List<CoinSelectionListItem> = emptyList()

    private val _coins: MutableStateFlow<List<CoinSelectionListItem>> = MutableStateFlow(emptyList())
    override val coins: StateFlow<List<CoinSelectionListItem>> = _coins.asStateFlow()

    private val _coinsCount: MutableStateFlow<Int> = MutableStateFlow(0)
    override val coinsCount: StateFlow<Int> = _coinsCount.asStateFlow()

    private val _summary: MutableStateFlow<CoinSelectionSummary> = MutableStateFlow(CoinSelectionSummary())
    override val summary: StateFlow<CoinSelectionSummary> = _summary.asStateFlow()

    private val _allVisibleCoinsSelected: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val allVisibleCoinsSelected: StateFlow<Boolean> = _allVisibleCoinsSelected.asStateFlow()

    private val _selectedFilters: MutableStateFlow<Set<CoinFilter>> = MutableStateFlow(emptySet())
    override val selectedFilters: StateFlow<Set<CoinFilter>> = _selectedFilters.asStateFlow()

    private val _availableFilters: MutableStateFlow<List<CoinFilter>> = MutableStateFlow(emptyList())
    override val availableFilters: StateFlow<List<CoinFilter>> = _availableFilters.asStateFlow()

    private val _selectedSort: MutableStateFlow<CoinSort> = MutableStateFlow(CoinSort.AMOUNT_HIGH_TO_LOW)
    override val selectedSort: StateFlow<CoinSort> = _selectedSort.asStateFlow()

    private val _hasError: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

    init {
        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_coin_selection),
                isCentered = true
            )
        }

        refreshCoins()
        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        when (event) {
            is LocalEvents.ToggleCoin -> {
                toggleCoin(event.id)
            }

            is LocalEvents.ApplyFilters -> {
                applyFilters(event.result)
            }

            is LocalEvents.SelectSort -> {
                selectSort(event.sort)
            }

            is LocalEvents.ToggleVisibleCoinsSelection -> {
                toggleVisibleCoinsSelection()
            }

            is LocalEvents.ConfirmSelection -> {
                confirmSelection()
            }

            is LocalEvents.OpenFilters -> {
                openFilters()
            }

            is LocalEvents.OpenSort -> {
                openSort()
            }

            is LocalEvents.OpenCoinInfo -> {
                openCoinInfo(event.coin)
            }

            is LocalEvents.Refresh -> {
                refreshCoins()
            }
        }
    }

    private fun refreshCoins() {
        _hasError.value = false
        val selectedIds = if (allCoins.isEmpty()) {
            selectedUtxoIds.toSet()
        } else {
            allCoins.filter { it.isSelected }.map { it.id }.toSet()
        }

        doAsync({
            val spendableUtxos = sendUseCase.getSpendableUtxosUseCase(
                session = session,
                accountAsset = selectedAccountAsset
            )

            CoinSelectionData(
                spendableUtxos = spendableUtxos,
                items = spendableUtxos.map { coin ->
                    CoinSelectionListItem(
                        id = coin.id,
                        amount = coin.utxo.satoshi.toAmountLook(
                            session = session,
                            assetId = coin.assetId,
                            denomination = displayDenomination,
                            withUnit = true,
                            withGrouping = true
                        ) ?: "${coin.utxo.satoshi}",
                        amountFiat = coin.utxo.satoshi.toAmountLook(
                            session = session,
                            assetId = coin.assetId,
                            denomination = Denomination.fiat(session),
                            withUnit = true,
                            withGrouping = true
                        ),
                        satoshi = coin.utxo.satoshi,
                        txHash = coin.utxo.txHash,
                        outputIndex = coin.utxo.index,
                        outpoint = coin.utxo.shortOutpoint(),
                        addressType = coin.utxo.addressType,
                        blockHeight = coin.utxo.blockHeight,
                        expiryHeight = coin.utxo.expiryHeight,
                        isBlinded = coin.utxo.isBlinded,
                        isConfirmed = (coin.utxo.blockHeight ?: 0L) > 0L,
                        isDust = coin.isDust,
                        isExpired = coin.isExpired,
                        isLegacyRecovery = coin.isLegacyRecovery,
                        isSelected = coin.id in selectedIds
                    )
                }
            )
        }, onSuccess = {
            spendableUtxos = it.spendableUtxos
            allCoins = it.items
            _coinsCount.value = allCoins.size
            updateAvailableFilters()
            applyFilterAndSort()
            updateSummary(allCoins)
            updateFilterAction()
        }, onError = {
            _hasError.value = true
            spendableUtxos = emptyList()
            allCoins = emptyList()
            _coinsCount.value = 0
            applyFilterAndSort()
            updateSummary(allCoins)
        })
    }

    private fun updateFilterAction() {
        _navData.value = _navData.value.copy(
            actions = if (_availableFilters.value.isEmpty()) {
                emptyList()
            } else {
                listOf(buildFilterAction())
            }
        )
    }

    private fun openCoinInfo(coin: CoinSelectionListItem) {
        postSideEffect(
            SideEffects.NavigateTo(
                NavigateDestinations.CoinInfo(
                    greenWallet = greenWallet,
                    accountAsset = selectedAccountAsset,
                    amount = coin.amount,
                    amountFiat = coin.amountFiat,
                    isConfirmed = coin.isConfirmed,
                    txHash = coin.txHash,
                    outputIndex = coin.outputIndex,
                    scriptType = coin.addressType,
                    blockHeight = coin.blockHeight,
                    isBlinded = coin.isBlinded == true
                )
            )
        )
    }

    private fun toggleCoin(id: String) {
        allCoins = allCoins.map {
            if (it.id == id) {
                it.copy(isSelected = !it.isSelected)
            } else {
                it
            }
        }
        applyFilterAndSort()

        viewModelScope.launch {
            updateSummary(allCoins)
        }
    }

    private fun toggleVisibleCoinsSelection() {
        val visibleCoinIds = _coins.value.map { it.id }.toSet()
        if (visibleCoinIds.isEmpty()) return

        val shouldSelect = !_allVisibleCoinsSelected.value
        allCoins = allCoins.map {
            if (it.id in visibleCoinIds) {
                it.copy(isSelected = shouldSelect)
            } else {
                it
            }
        }
        applyFilterAndSort()

        viewModelScope.launch {
            updateSummary(allCoins)
        }
    }

    private fun confirmSelection() {
        val selectedIds = allCoins.filter { it.isSelected }.map { it.id }.toSet()
        val selectedUtxos = spendableUtxos.filter { it.id in selectedIds }
        val selectedAmountSatoshi = selectedUtxos.takeIf { it.isNotEmpty() }?.sumOf { it.utxo.satoshi }

        NavigateDestinations.CoinSelection.setResult(
            CoinSelectionResult(
                selectedUtxoIds = selectedIds.toList(),
                selectedAmountSatoshi = selectedAmountSatoshi,
                gdkPayloadUtxos = selectedUtxos
                    .groupBy { it.assetId }
                    .mapValues { (_, coins) -> coins.map { it.rawUtxo } }
            )
        )
        postSideEffect(SideEffects.NavigateBack())
    }

    private fun applyFilters(result: CoinFilterResult) {
        _selectedFilters.value = result.filters.intersect(_availableFilters.value.toSet())
        _selectedSort.value = result.sort
        applyFilterAndSort()
        updateFilterBadge()
    }

    private fun selectSort(sort: CoinSort) {
        _selectedSort.value = sort
        applyFilterAndSort()
        updateFilterBadge()
    }

    private fun updateFilterBadge() {
        if (_navData.value.actions.isEmpty()) return
        _navData.value = _navData.value.copy(actions = listOf(buildFilterAction()))
    }

    private fun buildFilterAction(): NavAction {
        // Phosphor doesn't ship a funnel+dot icon, so the active state is the one
        // remaining custom asset; the inactive state uses the stock Phosphor icon.
        val isActive = _selectedFilters.value.isNotEmpty()
        return NavAction(
            icon = Res.drawable.funnel_outline_active.takeIf { isActive },
            imageVector = if (isActive) null else PhosphorIcons.Regular.Funnel,
            iconTint = if (isActive) Color.Unspecified else null,
            iconSize = 22.dp,
            isMenuEntry = false,
            onClick = {
                postEvent(LocalEvents.OpenFilters)
            }
        )
    }

    private fun openFilters() {
        if (availableFilters.value.isEmpty()) return

        postSideEffect(
            SideEffects.NavigateTo(
                NavigateDestinations.CoinFilters(
                    selectedFilters = selectedFilters.value.toList(),
                    availableFilters = availableFilters.value,
                    selectedSort = selectedSort.value
                )
            )
        )
    }

    private fun openSort() {
        postSideEffect(
            SideEffects.NavigateTo(
                NavigateDestinations.CoinSortSheet(selectedSort = selectedSort.value)
            )
        )
    }

    private fun updateAvailableFilters() {
        val filters = buildList {
            if (selectedAccountAsset.account.type != AccountType.TWO_OF_THREE &&
                selectedAccountAsset.account.type != AccountType.AMP_LEGACY_ACCOUNT
            ) {
                add(CoinFilter.EXPIRED)
            }
            if (selectedAccountAsset.account.isBitcoin) {
                add(CoinFilter.DUST)
            }
            if (selectedAccountAsset.account.isBitcoin &&
                selectedAccountAsset.account.type == AccountType.STANDARD
            ) {
                add(CoinFilter.LEGACY_RECOVERY)
            }
        }

        _availableFilters.value = filters
        _selectedFilters.value = _selectedFilters.value.intersect(filters.toSet())
    }

    private fun sortComparator(sort: CoinSort): Comparator<CoinSelectionListItem> = when (sort) {
        CoinSort.AMOUNT_HIGH_TO_LOW -> compareByDescending { it.satoshi }
        CoinSort.AMOUNT_LOW_TO_HIGH -> compareBy { it.satoshi }
        CoinSort.NEWEST -> compareBy<CoinSelectionListItem> { it.isConfirmed }
            .thenByDescending { it.blockHeight ?: 0L }
        CoinSort.OLDEST -> compareByDescending<CoinSelectionListItem> { it.isConfirmed }
            .thenBy { it.blockHeight ?: 0L }
    }

    private fun applyFilterAndSort() {
        _coins.value = allCoins.filter { coin ->
            _selectedFilters.value.isEmpty() || _selectedFilters.value.any { coin.matchesFilter(it) }
        }.sortedWith(sortComparator(_selectedSort.value))
        _allVisibleCoinsSelected.value = _coins.value.isNotEmpty() && _coins.value.all { it.isSelected }
    }

    private fun CoinSelectionListItem.matchesFilter(filter: CoinFilter): Boolean = when (filter) {
        CoinFilter.DUST -> isDust
        CoinFilter.EXPIRED -> isExpired
        CoinFilter.LEGACY_RECOVERY -> isLegacyRecovery
    }

    private suspend fun updateSummary(coins: List<CoinSelectionListItem>) {
        val selected = coins.filter { it.isSelected }
        val selectedAmountSatoshi = selected.sumOf { it.satoshi }
        _summary.value = CoinSelectionSummary(
            count = selected.size,
            amount = selectedAmountSatoshi.toAmountLook(
                session = session,
                assetId = selectedAccountAsset.assetId,
                denomination = displayDenomination,
                withUnit = true,
                withGrouping = true
            ),
            amountFiat = selectedAmountSatoshi.toAmountLook(
                session = session,
                assetId = selectedAccountAsset.assetId,
                denomination = Denomination.fiat(session),
                withUnit = true,
                withGrouping = true
            )?.let { "≈ $it" },
            canConfirm = true
        )
    }
}

class CoinSelectionViewModelPreview(
    greenWallet: GreenWallet,
    accountAsset: AccountAsset
) : CoinSelectionViewModelAbstract(greenWallet = greenWallet, accountAsset = accountAsset) {
    override val coins: StateFlow<List<CoinSelectionListItem>> = MutableStateFlow(
        listOf(
            CoinSelectionListItem(
                id = "coin-1",
                amount = "0.015 BTC",
                amountFiat = "1,500.00 USD",
                satoshi = 1_500_000,
                txHash = "3a5f1e2b4c6d7e8f90123456789abcdef123456789abcdef123456789c8d7a6f",
                outputIndex = 0,
                outpoint = "3a5f1e2b...9c8d7a6f:0",
                addressType = "p2wsh",
                blockHeight = 860_000,
                expiryHeight = null,
                isBlinded = false,
                isConfirmed = true,
                isExpired = true,
                isLegacyRecovery = true,
                isSelected = true
            ),
            CoinSelectionListItem(
                id = "coin-2",
                amount = "0.004 BTC",
                amountFiat = "400.00 USD",
                satoshi = 400_000,
                txHash = "0f4b12aa4c6d7e8f90123456789abcdef123456789abcdef123456777c390de",
                outputIndex = 1,
                outpoint = "0f4b12aa...77c390de:1",
                addressType = "csv",
                blockHeight = null,
                expiryHeight = null,
                isBlinded = false,
                isConfirmed = false,
                isDust = true
            )
        )
    )
    override val coinsCount: StateFlow<Int> = MutableStateFlow(coins.value.size)
    override val summary: StateFlow<CoinSelectionSummary> = MutableStateFlow(
        CoinSelectionSummary(
            count = 1,
            amount = "0.015 BTC",
            amountFiat = "≈ 1,500.00 USD",
            canConfirm = true
        )
    )
    override val allVisibleCoinsSelected: StateFlow<Boolean> = MutableStateFlow(false)
    override val selectedFilters: StateFlow<Set<CoinFilter>> = MutableStateFlow(emptySet())
    override val availableFilters: StateFlow<List<CoinFilter>> = MutableStateFlow(
        listOf(CoinFilter.EXPIRED, CoinFilter.DUST, CoinFilter.LEGACY_RECOVERY)
    )
    override val selectedSort: StateFlow<CoinSort> = MutableStateFlow(CoinSort.AMOUNT_HIGH_TO_LOW)
    override val hasError: StateFlow<Boolean> = MutableStateFlow(false)

    companion object {
        fun preview() = CoinSelectionViewModelPreview(
            greenWallet = previewWallet(),
            accountAsset = previewAccountAsset()
        )
    }
}
