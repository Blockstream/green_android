package com.blockstream.compose.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.check_circle
import blockstream_green.common.generated.resources.id_amount_high_to_low
import blockstream_green.common.generated.resources.id_amount_low_to_high
import blockstream_green.common.generated.resources.id_newest
import blockstream_green.common.generated.resources.id_oldest
import blockstream_green.common.generated.resources.id_sort
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.models.send.CoinSort
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.setResult
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteHigh
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CoinSortBottomSheet(
    selectedSort: CoinSort,
    onDismissRequest: () -> Unit
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_sort),
        titleTextAlign = TextAlign.Start,
        withHorizontalPadding = false,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismissRequest
    ) {
        Column {
            val sorts = CoinSort.entries
            sorts.forEachIndexed { index, sort ->
                CoinSortRow(
                    sort = sort,
                    isSelected = sort == selectedSort,
                    onClick = {
                        NavigateDestinations.CoinSortSheet.setResult(sort)
                        onDismissRequest()
                    }
                )

                if (index < sorts.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CoinSortRow(
    sort: CoinSort,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        GreenRow(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(sort.title()),
                    style = titleSmall,
                    color = if (isSelected) green else whiteHigh
                )
            }

            if (isSelected) {
                Icon(
                    painter = painterResource(Res.drawable.check_circle),
                    contentDescription = null,
                    tint = green,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }
        }
    }
}

private fun CoinSort.title() = when (this) {
    CoinSort.AMOUNT_HIGH_TO_LOW -> Res.string.id_amount_high_to_low
    CoinSort.AMOUNT_LOW_TO_HIGH -> Res.string.id_amount_low_to_high
    CoinSort.NEWEST -> Res.string.id_newest
    CoinSort.OLDEST -> Res.string.id_oldest
}
