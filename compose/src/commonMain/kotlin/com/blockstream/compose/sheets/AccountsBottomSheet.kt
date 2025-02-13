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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_no_available_accounts
import com.blockstream.common.gdk.data.AccountAssetBalanceList
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.navigation.setResult
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsBottomSheet(
    viewModel: GreenViewModel,
    accountsBalance: AccountAssetBalanceList,
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
            accountsBalance.list.forEach { accountAssetBalance ->
                GreenAccountAsset(accountAssetBalance = accountAssetBalance, session = viewModel.sessionOrNull, withAsset = withAsset) {
                    NavigateDestinations.Accounts.setResult(accountAssetBalance)
                    onDismissRequest()
                }
            }

            if(accountsBalance.list.isEmpty()){
                Text(
                    text = stringResource(Res.string.id_no_available_accounts),
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