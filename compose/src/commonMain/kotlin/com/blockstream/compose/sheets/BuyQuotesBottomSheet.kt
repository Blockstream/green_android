package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_provider
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.MeldProvider
import com.blockstream.green.data.meld.data.QuoteResponse
import com.blockstream.ui.navigation.setResult
import com.blockstream.ui.utils.plus
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyQuotesBottomSheet(
    viewModel: GreenViewModel,
    quotes: List<QuoteResponse>,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_provider),
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
            items(quotes) { quote ->
                MeldProvider(
                    title = null,
                    quote = quote,
                    onProgress = false,
                    withEditIcon = false,
                    onClick = {
                        NavigateDestinations.BuyQuotes.setResult(quote)
                        onDismissRequest()
                    }
                )
            }
        }

    }
}