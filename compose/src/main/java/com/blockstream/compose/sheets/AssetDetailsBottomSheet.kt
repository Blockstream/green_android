package com.blockstream.compose.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.koin.getScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.BTC_POLICY_ASSET
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.ScanResult
import com.blockstream.common.events.Events
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.models.sheets.AssetDetailsViewModel
import com.blockstream.common.models.sheets.AssetDetailsViewModelAbstract
import com.blockstream.common.models.sheets.AssetDetailsViewModelPreview
import com.blockstream.common.models.sheets.NoteViewModel
import com.blockstream.common.models.sheets.NoteViewModelAbstract
import com.blockstream.common.models.sheets.NoteViewModelPreview
import com.blockstream.common.models.sheets.TransactionDetailsViewModel
import com.blockstream.common.models.sheets.TransactionDetailsViewModelAbstract
import com.blockstream.common.models.sheets.TransactionDetailsViewModelPreview
import com.blockstream.common.models.wallet.WalletNameViewModel
import com.blockstream.common.models.wallet.WalletNameViewModelAbstract
import com.blockstream.common.models.wallet.WalletNameViewModelPreview
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.extensions.onTextFieldValueChange
import com.blockstream.compose.extensions.onValueChange
import com.blockstream.compose.extensions.random
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.navigation.setNavigationResult
import com.blockstream.compose.sheets.AnalyticsBottomSheet.sheetState
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.CopyContainer
import com.blockstream.compose.utils.OpenKeyboard
import com.blockstream.compose.utils.copyToClipboard
import com.blockstream.compose.utils.noRippleClickable
import com.blockstream.compose.utils.stringResourceId
import com.blockstream.compose.views.GreenBottomSheet
import org.koin.core.parameter.parametersOf

@Parcelize
data class AssetDetailsBottomSheet(
    val assetId: String = BTC_POLICY_ASSET,
    val accountAsset: AccountAsset? = null,
    val greenWallet: GreenWallet
) : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<AssetDetailsViewModel> {
            parametersOf(assetId, accountAsset, greenWallet)
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
        title = stringResource(id = R.string.id_asset_details),
        viewModel = viewModel,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest
    ) {
        val data by viewModel.data.collectAsStateWithLifecycle()

        GreenColumn(padding = 0, space = 16, modifier = Modifier.padding(top = 16.dp)) {
            data.forEach { pair ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResourceId(id = pair.first),
                        style = labelMedium,
                        color = whiteHigh
                    )
                    stringResourceId(id = pair.second).also {
                        CopyContainer(value = it) {
                            Text(text = it, style = bodyLarge, color = whiteMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun AssetDetailsBottomSheetPreview() {
    GreenPreview {
        AssetDetailsBottomSheet(
            viewModel = AssetDetailsViewModelPreview.preview(),
            onDismissRequest = { }
        )
    }
}