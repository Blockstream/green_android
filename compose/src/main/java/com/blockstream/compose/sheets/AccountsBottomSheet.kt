package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.koin.koinScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.previewAccountAssetBalance
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.common.models.SimpleGreenViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.navigation.setNavigationResult
import com.blockstream.compose.theme.bodyMedium
import org.koin.core.parameter.parametersOf

@Parcelize
data class AccountsBottomSheet(
    val greenWallet: GreenWallet,
    val accountsBalance: List<AccountAssetBalance>,
    val withAsset: Boolean
) : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SimpleGreenViewModel> {
            parametersOf(greenWallet)
        }

        AccountsBottomSheet(
            viewModel = viewModel,
            accountsBalance = accountsBalance,
            withAsset = withAsset,
            onDismissRequest = onDismissRequest()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsBottomSheet(
    viewModel: GreenViewModel,
    accountsBalance: List<AccountAssetBalance>,
    withAsset: Boolean,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false,
        ),
        onDismissRequest = onDismissRequest
    ) {

        GreenColumn(
            padding = 0, space = 8, modifier = Modifier
                .padding(top = 16.dp)
                .verticalScroll(
                    rememberScrollState()
                )
        ) {
            accountsBalance.forEach { accountAssetBalance ->
                GreenAccountAsset(accountAssetBalance = accountAssetBalance, session = viewModel.sessionOrNull, withAsset = withAsset) {
                    setNavigationResult(AccountsBottomSheet::class.resultKey, accountAssetBalance)
                    onDismissRequest()
                }
            }

            if(accountsBalance.isEmpty()){
                Text(
                    text = stringResource(R.string.id_no_available_accounts),
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

@Composable
@Preview
fun AccountsBottomSheetPreview() {
    GreenPreview {
        AccountsBottomSheet(
            viewModel = SimpleGreenViewModelPreview(previewWallet()),
            accountsBalance = listOf(previewAccountAssetBalance()),
            withAsset = true,
            onDismissRequest = { }
        )
    }
}