package com.blockstream.compose.screens.send

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.FunnelX
import com.adamglin.phosphoricons.regular.X
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_amount_high_to_low
import blockstream_green.common.generated.resources.id_amount_low_to_high
import blockstream_green.common.generated.resources.id_choose_which_coins_to_use
import blockstream_green.common.generated.resources.id_coins
import blockstream_green.common.generated.resources.id_confirm
import blockstream_green.common.generated.resources.id_confirmed
import blockstream_green.common.generated.resources.id_d_coin_selected
import blockstream_green.common.generated.resources.id_d_coins_selected
import blockstream_green.common.generated.resources.id_error
import blockstream_green.common.generated.resources.id_newest
import blockstream_green.common.generated.resources.id_no_coins_available
import blockstream_green.common.generated.resources.id_no_coins_match_your_filters
import blockstream_green.common.generated.resources.id_no_utxos_found
import blockstream_green.common.generated.resources.id_oldest
import blockstream_green.common.generated.resources.id_reset_filters
import blockstream_green.common.generated.resources.id_retry
import blockstream_green.common.generated.resources.id_select_all
import blockstream_green.common.generated.resources.id_try_adjusting_or_resetting_your_filters
import blockstream_green.common.generated.resources.id_unable_to_load_coins
import blockstream_green.common.generated.resources.id_unconfirmed
import blockstream_green.common.generated.resources.id_unselect_all
import blockstream_green.common.generated.resources.id_using_all_available_coins
import blockstream_green.common.generated.resources.info
import com.adamglin.phosphoricons.regular.SortAscending
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenDataLayout
import com.blockstream.compose.models.send.CoinFilterResult
import com.blockstream.compose.models.send.CoinSelectionListItem
import com.blockstream.compose.models.send.CoinSelectionViewModelAbstract
import com.blockstream.compose.models.send.CoinSelectionViewModelPreview
import com.blockstream.compose.models.send.CoinSort
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.getResult
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.md_theme_surfaceCircle
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.SetupScreen
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private enum class CoinsContentState {
    LIST, FILTERED_EMPTY, NO_COINS, ERROR
}

@Composable
fun CoinSelectionScreen(
    viewModel: CoinSelectionViewModelAbstract
) {
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            viewModel.postEvent(CoinSelectionViewModelAbstract.LocalEvents.Refresh)
            delay(500.toDuration(DurationUnit.MILLISECONDS))
            isRefreshing = false
        }
    }

    SetupScreen(viewModel = viewModel) {
        val coins by viewModel.coins.collectAsStateWithLifecycle()
        val coinsCount by viewModel.coinsCount.collectAsStateWithLifecycle()
        val summary by viewModel.summary.collectAsStateWithLifecycle()
        val allVisibleCoinsSelected by viewModel.allVisibleCoinsSelected.collectAsStateWithLifecycle()
        val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()
        val hasError by viewModel.hasError.collectAsStateWithLifecycle()
        val selectedFilters by viewModel.selectedFilters.collectAsStateWithLifecycle()

        NavigateDestinations.CoinFilters.getResult<CoinFilterResult> {
            viewModel.postEvent(CoinSelectionViewModelAbstract.LocalEvents.ApplyFilters(it))
        }

        NavigateDestinations.CoinSortSheet.getResult<CoinSort> {
            viewModel.postEvent(CoinSelectionViewModelAbstract.LocalEvents.SelectSort(it))
        }

        val contentState = when {
            hasError -> CoinsContentState.ERROR
            coinsCount == 0 -> CoinsContentState.NO_COINS
            coins.isEmpty() -> CoinsContentState.FILTERED_EMPTY
            else -> CoinsContentState.LIST
        }

        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
            }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = stringResource(Res.string.id_choose_which_coins_to_use),
                    style = bodyLarge,
                    color = whiteMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.id_coins),
                        style = bodyLarge,
                        color = whiteMedium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                viewModel.postEvent(CoinSelectionViewModelAbstract.LocalEvents.OpenSort)
                            }
                            .heightIn(min = 32.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = stringResource(selectedSort.title()),
                            style = bodyLarge,
                            color = green
                        )
                        Icon(
                            imageVector = PhosphorIcons.Regular.SortAscending,
                            contentDescription = null,
                            tint = green,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                AnimatedContent(
                    targetState = contentState,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(150))
                    },
                    label = "CoinsListState"
                ) { state ->
                    when (state) {
                        CoinsContentState.ERROR -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.X,
                                    contentDescription = null,
                                    tint = whiteMedium,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = stringResource(Res.string.id_error),
                                    style = bodyLarge,
                                    color = whiteHigh,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                                Text(
                                    text = stringResource(Res.string.id_unable_to_load_coins),
                                    style = bodyLarge,
                                    color = whiteMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                                Text(
                                    text = stringResource(Res.string.id_retry),
                                    style = bodyLarge,
                                    color = green,
                                    modifier = Modifier
                                        .padding(top = 12.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable {
                                            viewModel.postEvent(CoinSelectionViewModelAbstract.LocalEvents.Refresh)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        CoinsContentState.NO_COINS -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(Res.string.id_no_utxos_found),
                                    style = bodyMedium,
                                    color = whiteMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        CoinsContentState.FILTERED_EMPTY -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.FunnelX,
                                    contentDescription = null,
                                    tint = whiteMedium,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = stringResource(Res.string.id_no_coins_match_your_filters),
                                    style = bodyLarge,
                                    color = whiteHigh,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                                Text(
                                    text = stringResource(Res.string.id_try_adjusting_or_resetting_your_filters),
                                    style = bodyLarge,
                                    color = whiteMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                                Text(
                                    text = stringResource(Res.string.id_reset_filters),
                                    style = bodyLarge,
                                    color = green,
                                    modifier = Modifier
                                        .padding(top = 12.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable {
                                            viewModel.postEvent(
                                                CoinSelectionViewModelAbstract.LocalEvents.ApplyFilters(
                                                    CoinFilterResult(
                                                        filters = emptySet(),
                                                        sort = selectedSort
                                                    )
                                                )
                                            )
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        CoinsContentState.LIST -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(coins) { coin ->
                                    CoinSelectionCard(
                                        coin = coin,
                                        onClick = {
                                            viewModel.postEvent(
                                                CoinSelectionViewModelAbstract.LocalEvents.ToggleCoin(
                                                    coin.id
                                                )
                                            )
                                        },
                                        onInfoClick = {
                                            viewModel.postEvent(
                                                CoinSelectionViewModelAbstract.LocalEvents.OpenCoinInfo(
                                                    coin
                                                )
                                            )
                                        }
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(
                                    if (summary.count == 1) Res.string.id_d_coin_selected else Res.string.id_d_coins_selected,
                                    summary.count
                                ),
                                style = bodyMedium,
                                color = whiteMedium
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .offset(x = (-8).dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable(enabled = coins.isNotEmpty()) {
                                        viewModel.postEvent(
                                            CoinSelectionViewModelAbstract.LocalEvents.ToggleVisibleCoinsSelection
                                        )
                                    }
                                    .heightIn(min = 32.dp)
                                    .padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        if (allVisibleCoinsSelected) Res.string.id_unselect_all else Res.string.id_select_all
                                    ),
                                    style = bodyMedium,
                                    color = if (coins.isEmpty()) whiteLow else green
                                )
                            }
                        }

                        if (summary.count > 0) {
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                summary.amount?.also {
                                    Text(
                                        text = it,
                                        style = titleSmall,
                                        color = whiteHigh
                                    )
                                }
                                summary.amountFiat?.also {
                                    Text(
                                        text = it,
                                        style = bodyMedium,
                                        color = whiteMedium
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(
                                    if (coins.isEmpty()) Res.string.id_no_coins_available else Res.string.id_using_all_available_coins
                                ),
                                style = labelMedium,
                                color = whiteHigh
                            )
                        }
                    }

                    GreenButton(
                        text = stringResource(Res.string.id_confirm),
                        enabled = summary.canConfirm,
                        size = GreenButtonSize.BIG,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        viewModel.postEvent(CoinSelectionViewModelAbstract.LocalEvents.ConfirmSelection)
                    }
            }
        }
    }
}

@Composable
private fun CoinSelectionCard(
    coin: CoinSelectionListItem,
    onClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    GreenDataLayout(
        withPadding = false,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp)
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinCheckbox(checked = coin.isSelected, onClick = onClick)
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = coin.outpoint,
                        style = labelMedium,
                        color = whiteHigh,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(
                            if (coin.isConfirmed) Res.string.id_confirmed else Res.string.id_unconfirmed
                        ),
                        style = bodyMedium,
                        color = whiteMedium
                    )
                }
                Spacer(modifier = Modifier.width(30.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = coin.amount,
                        style = labelMedium,
                        color = whiteHigh
                    )
                    coin.amountFiat?.also {
                        Text(
                            text = it,
                            style = bodyMedium,
                            color = whiteMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onInfoClick) {
                    Icon(
                        painter = painterResource(Res.drawable.info),
                        contentDescription = null,
                        tint = whiteHigh
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}

@Composable
private fun CoinCheckbox(checked: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(2.dp))
            .border(
                width = 1.5.dp,
                color = if (checked) whiteHigh else md_theme_surfaceCircle,
                shape = RoundedCornerShape(2.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = whiteHigh,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun CoinSort.title() = when (this) {
    CoinSort.AMOUNT_HIGH_TO_LOW -> Res.string.id_amount_high_to_low
    CoinSort.AMOUNT_LOW_TO_HIGH -> Res.string.id_amount_low_to_high
    CoinSort.NEWEST -> Res.string.id_newest
    CoinSort.OLDEST -> Res.string.id_oldest
}

@Preview
@Composable
fun CoinSelectionScreenPreview() {
    GreenPreview {
        CoinSelectionScreen(viewModel = CoinSelectionViewModelPreview.preview())
    }
}

@Preview
@Composable
fun CoinSelectionCardsPreview() {
    GreenPreview {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CoinSelectionCard(
                coin = CoinSelectionListItem(
                    id = "coin-without-label",
                    amount = "2 226 sats",
                    amountFiat = "1.38 USD",
                    satoshi = 2_226,
                    txHash = "a06ee2f94c6d7e8f90123456789abcdef123456789abcdef1234567895a849ed",
                    outputIndex = 0,
                    outpoint = "a06ee2f9...5a849ed:0",
                    addressType = "p2wpkh",
                    blockHeight = 860_000,
                    expiryHeight = null,
                    isBlinded = false,
                    isConfirmed = true,
                    isSelected = false
                ),
                onClick = {},
                onInfoClick = {}
            )

            CoinSelectionCard(
                coin = CoinSelectionListItem(
                    id = "coin-dust",
                    amount = "546 sats",
                    amountFiat = "0.33 USD",
                    satoshi = 546,
                    txHash = "4ac7d6f84c6d7e8f90123456789abcdef123456789abcdef123456789ed6af123",
                    outputIndex = 1,
                    outpoint = "4ac7d6f8...ed6af123:1",
                    addressType = "p2wsh",
                    blockHeight = null,
                    expiryHeight = null,
                    isBlinded = false,
                    isConfirmed = false,
                    isDust = true,
                    isSelected = false
                ),
                onClick = {},
                onInfoClick = {}
            )

            CoinSelectionCard(
                coin = CoinSelectionListItem(
                    id = "coin-expired",
                    amount = "2 226 sats",
                    amountFiat = "1.38 USD",
                    satoshi = 2_226,
                    txHash = "5ccf420f4c6d7e8f90123456789abcdef123456789abcdef123456789bbf1208c",
                    outputIndex = 0,
                    outpoint = "5ccf420f...bbf1208c:0",
                    addressType = "p2wsh",
                    blockHeight = 860_000,
                    expiryHeight = null,
                    isBlinded = false,
                    isConfirmed = true,
                    isExpired = true,
                    isSelected = true
                ),
                onClick = {},
                onInfoClick = {}
            )

            CoinSelectionCard(
                coin = CoinSelectionListItem(
                    id = "coin-expired-dust",
                    amount = "900 sats",
                    amountFiat = "0.55 USD",
                    satoshi = 900,
                    txHash = "9f1a1eb94c6d7e8f90123456789abcdef123456789abcdef123456789bcb168aa",
                    outputIndex = 2,
                    outpoint = "9f1a1eb9...bcb168aa:2",
                    addressType = "p2wsh",
                    blockHeight = 860_000,
                    expiryHeight = null,
                    isBlinded = false,
                    isConfirmed = true,
                    isDust = true,
                    isExpired = true,
                    isSelected = false
                ),
                onClick = {},
                onInfoClick = {}
            )
        }
    }
}
