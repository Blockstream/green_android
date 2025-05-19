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
import blockstream_green.common.generated.resources.id_account_selector
import blockstream_green.common.generated.resources.id_no_available_accounts
import com.blockstream.common.gdk.data.AccountAssetBalanceList
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.whiteLow
import com.blockstream.ui.components.GreenColumn
import com.blockstream.ui.navigation.setResult
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsBottomSheet(
    viewModel: GreenViewModel,
    accountsBalance: AccountAssetBalanceList,
    title: String? = null,
    message: String? = null,
    withAsset: Boolean = true,
    withAssetIcon: Boolean = true,
    withArrow: Boolean = false,
    withAction: Boolean = true,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = title ?: stringResource(Res.string.id_account_selector),
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false,
        ),
        onDismissRequest = onDismissRequest
    ) {

        GreenColumn(
            padding = 0,
            space = 8,
            modifier = Modifier
                .verticalScroll(
                    rememberScrollState()
                )
        ) {
            accountsBalance.list.forEach { accountAssetBalance ->
                GreenAccountAsset(
                    accountAssetBalance = accountAssetBalance,
                    session = viewModel.sessionOrNull,
                    withAsset = withAsset,
                    withAssetIcon = withAssetIcon,
                    withArrow = withArrow,
                    onClick = {
                        NavigateDestinations.Accounts.setResult(accountAssetBalance)
                        onDismissRequest()
                    }.takeIf { withAction }
                )
            }

            if (accountsBalance.list.isEmpty()) {
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

            message?.also {
                Text(
                    text = it,
                    color = whiteLow,
                    style = bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )
            }
        }

    }
}