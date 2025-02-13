package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_empty
import com.blockstream.common.extensions.isBlank
import com.blockstream.common.gdk.data.AssetBalanceList
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenAsset
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenSearchField
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.navigation.setResult
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsBottomSheet(
    viewModel: GreenViewModel,
    assetBalance: AssetBalanceList,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        viewModel = viewModel,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
        onDismissRequest = onDismissRequest
    ) {

        var query by remember { mutableStateOf("") }
        val data by remember {
            derivedStateOf {
                if (query.isBlank()) {
                    assetBalance.list
                } else {
                    assetBalance.list.filter {
                        it.asset.name(viewModel.sessionOrNull).fallbackString().contains(query) || it.assetId.contains(query)
                    }
                }
            }
        }

        GreenColumn(
            padding = 0, space = 8, modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxSize()
        ) {
            GreenSearchField(
                value = query,
                onValueChange = {
                    query = it
                }
            )

            GreenColumn(
                padding = 0,
                space = 4,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp)
            ) {
                data.forEach { assetBalance ->
                    GreenAsset(assetBalance = assetBalance, session = viewModel.sessionOrNull) {
                        NavigateDestinations.Assets.setResult(assetBalance)
                        onDismissRequest()
                    }
                }
            }


            if (data.isEmpty()) {
                Text(
                    text = stringResource(Res.string.id_empty),
                    style = bodyMedium,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}