package com.blockstream.compose.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_2fa_expired
import blockstream_green.common.generated.resources.id_apply
import blockstream_green.common.generated.resources.id_coin_type_d_of_d
import blockstream_green.common.generated.resources.id_dust
import blockstream_green.common.generated.resources.id_filter
import blockstream_green.common.generated.resources.id_legacy_recovery
import blockstream_green.common.generated.resources.id_reset
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowsCounterClockwise
import com.adamglin.phosphoricons.regular.Coins
import com.adamglin.phosphoricons.regular.Warning
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.models.send.CoinFilter
import com.blockstream.compose.models.send.CoinFilterResult
import com.blockstream.compose.models.send.CoinSort
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.setResult
import com.blockstream.compose.theme.blueOutline
import com.blockstream.compose.theme.blueSurface
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.md_theme_outline
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import org.jetbrains.compose.resources.stringResource

@Composable
fun CoinFilterBottomSheet(
    selectedFilters: Set<CoinFilter>,
    availableFilters: List<CoinFilter>,
    selectedSort: CoinSort,
    onDismissRequest: () -> Unit
) {
    var pendingFilters by remember { mutableStateOf(selectedFilters) }

    GreenBottomSheet(
        title = stringResource(Res.string.id_filter),
        titleTextAlign = TextAlign.Start,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismissRequest
    ) {
        GreenColumn(padding = 0, space = 26) {
            Text(
                text = stringResource(Res.string.id_coin_type_d_of_d, pendingFilters.size, availableFilters.size),
                style = bodyMedium,
                color = whiteMedium
            )

            GreenColumn(padding = 0, space = 8) {
                availableFilters.forEach { filter ->
                    CoinFilterItem(
                        filter = filter,
                        isSelected = filter in pendingFilters,
                        onClick = {
                            pendingFilters = if (filter in pendingFilters) {
                                pendingFilters - filter
                            } else {
                                pendingFilters + filter
                            }
                        }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GreenButton(
                    text = stringResource(Res.string.id_apply),
                    size = GreenButtonSize.BIG,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NavigateDestinations.CoinFilters.setResult(
                        CoinFilterResult(filters = pendingFilters, sort = selectedSort)
                    )
                    onDismissRequest()
                }

                GreenButton(
                    text = stringResource(Res.string.id_reset),
                    type = GreenButtonType.OUTLINE,
                    size = GreenButtonSize.BIG,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NavigateDestinations.CoinFilters.setResult(
                        CoinFilterResult(filters = emptySet(), sort = selectedSort)
                    )
                    onDismissRequest()
                }
            }
        }
    }
}

@Composable
private fun CoinFilterItem(
    filter: CoinFilter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) blueSurface else Color.Transparent,
        border = BorderStroke(1.dp, if (isSelected) blueOutline else md_theme_outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = filter.icon(),
                contentDescription = null,
                tint = if (isSelected) green else whiteMedium,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(filter.title()),
                style = bodyMedium,
                color = if (isSelected) green else whiteHigh
            )
        }
    }
}

private fun CoinFilter.title() = when (this) {
    CoinFilter.DUST -> Res.string.id_dust
    CoinFilter.EXPIRED -> Res.string.id_2fa_expired
    CoinFilter.LEGACY_RECOVERY -> Res.string.id_legacy_recovery
}

private fun CoinFilter.icon(): ImageVector = when (this) {
    CoinFilter.DUST -> PhosphorIcons.Regular.Coins
    CoinFilter.EXPIRED -> PhosphorIcons.Regular.Warning
    CoinFilter.LEGACY_RECOVERY -> PhosphorIcons.Regular.ArrowsCounterClockwise
}
