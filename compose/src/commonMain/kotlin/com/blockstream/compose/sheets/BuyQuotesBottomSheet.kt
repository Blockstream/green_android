package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_best_price
import blockstream_green.common.generated.resources.id_exchange
import blockstream_green.common.generated.resources.id_other
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.MeldProvider
import com.blockstream.compose.models.GreenViewModel
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.setResult
import com.blockstream.compose.utils.ifTrue
import com.blockstream.compose.utils.plus
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyQuotesBottomSheet(
    viewModel: GreenViewModel,
    quotes: List<com.blockstream.data.meld.data.QuoteResponse>,
    selectedServiceProvider: String? = null,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_exchange),
        viewModel = viewModel,
        withHorizontalPadding = false,
        withBottomPadding = false,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false
        ),
        onDismissRequest = onDismissRequest
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp) + PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            itemsIndexed(quotes) { index, quote ->

                val title = if (index == 0) {
                    stringResource(Res.string.id_best_price)
                } else if (index == 1) {
                    stringResource(Res.string.id_other)
                } else null

                MeldProvider(
                    title = title,
                    badge = stringResource(Res.string.id_best_price).takeIf { index == 0 },
                    quote = quote,
                    onProgress = false,
                    withEditIcon = false,
                    isChecked = selectedServiceProvider == quote.serviceProvider,
                    modifier = Modifier.ifTrue(index == 1) {
                        it.padding(top = 8.dp)
                    },
                    onClick = {
                        NavigateDestinations.BuyQuotes.setResult(quote)
                        onDismissRequest()
                    }
                )
            }
        }
    }
}