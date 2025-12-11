package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_more_details
import com.blockstream.common.models.sheets.TransactionDetailsViewModelAbstract
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.views.DataListItem
import com.blockstream.compose.components.GreenColumn
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsBottomSheet(
    viewModel: TransactionDetailsViewModelAbstract,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_more_details),
        viewModel = viewModel,
        onDismissRequest = onDismissRequest
    ) {

        val data by viewModel.data.collectAsStateWithLifecycle()

        GreenColumn(
            padding = 0, space = 16, modifier = Modifier
                .padding(top = 16.dp)
                .verticalScroll(
                    rememberScrollState()
                )
        ) {
            data.forEachIndexed { index, pair ->
                DataListItem(
                    title = pair.first,
                    data = pair.second,
                    withDivider = index < data.size - 1
                )
            }
        }
    }
}