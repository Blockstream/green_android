package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blockstream.common.gdk.data.AccountAssetBalanceList
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.navigation.setResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsAccountsBottomSheet(
    viewModel: GreenViewModel,
    assetsAccounts: AccountAssetBalanceList,
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
            assetsAccounts.list.forEach { account ->
                GreenAccountAsset(accountAssetBalance = account, session = viewModel.sessionOrNull) {
                    NavigateDestinations.AssetsAccounts.setResult(account)
                    onDismissRequest()
                }
            }
        }

    }
}