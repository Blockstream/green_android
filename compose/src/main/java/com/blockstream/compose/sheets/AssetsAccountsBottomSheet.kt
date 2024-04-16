package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.koin.getScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.previewAccount
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAssetBalance
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.models.SimpleGreenViewModel
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.navigation.setNavigationResult
import org.koin.core.parameter.parametersOf

@Parcelize
data class AssetsAccountsBottomSheet(
    val greenWallet: GreenWallet,
    val assetsAccounts: List<AccountAssetBalance>
) : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<SimpleGreenViewModel> {
            parametersOf(greenWallet)
        }

        AssetsAccountsBottomSheet(
            viewModel = viewModel,
            assetsAccounts = assetsAccounts,
            onDismissRequest = onDismissRequest()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsAccountsBottomSheet(
    viewModel: GreenViewModel,
    assetsAccounts: List<AccountAssetBalance>,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
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
            assetsAccounts.forEach { account ->
                GreenAccountAsset(accountAssetBalance = account, session = viewModel.sessionOrNull) {
                    setNavigationResult(AssetsAccountsBottomSheet::class.resultKey, account)
                    onDismissRequest()
                }
            }
        }

    }
}

@Composable
@Preview
fun AssetsAccountBottomSheetPreview() {
    GreenPreview {
        AssetsAccountsBottomSheet(
            viewModel = GreenViewModel(previewWallet()),
            assetsAccounts = listOf(previewAccount()).map {
                AccountAssetBalance.create(accountAsset = it.accountAsset)
            },
            onDismissRequest = { }
        )
    }
}