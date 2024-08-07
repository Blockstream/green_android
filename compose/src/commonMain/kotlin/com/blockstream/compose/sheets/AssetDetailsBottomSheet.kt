package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_asset_details
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.models.sheets.AssetDetailsViewModel
import com.blockstream.common.models.sheets.AssetDetailsViewModelAbstract
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.CopyContainer
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

@Parcelize
data class AssetDetailsBottomSheet(
    val greenWallet: GreenWallet,
    val assetId: String = BTC_POLICY_ASSET,
    val accountAsset: AccountAsset? = null
) : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<AssetDetailsViewModel> {
            parametersOf(greenWallet, assetId, accountAsset)
        }

        AssetDetailsBottomSheet(
            viewModel = viewModel,
            sheetState = sheetState(skipPartiallyExpanded = true),
            onDismissRequest = onDismissRequest()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailsBottomSheet(
    viewModel: AssetDetailsViewModelAbstract,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_asset_details),
        viewModel = viewModel,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest
    ) {
        val data by viewModel.data.collectAsStateWithLifecycle()

        GreenColumn(padding = 0, space = 16, modifier = Modifier.padding(top = 16.dp)) {
            data.forEach { pair ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = pair.first.string(),
                        style = labelMedium,
                        color = whiteHigh
                    )
                    pair.second.string().also {
                        CopyContainer(value = it) {
                            Text(text = it, style = bodyLarge, color = whiteMedium)
                        }
                    }
                }
            }
        }
    }
}