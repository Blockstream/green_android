package com.blockstream.compose.screens.receive

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.blockstream.common.models.receive.ReceiveChooseAccountViewModelAbstract
import com.blockstream.common.models.receive.ReceiveChooseAccountViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.ui.components.GreenColumn
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ReceiveChooseAccountScreen(
    viewModel: ReceiveChooseAccountViewModelAbstract
) {

    SetupScreen(
        viewModel = viewModel
    ) {
        val accounts = viewModel.accounts

        GreenColumn(padding = 0, space = 4, modifier = Modifier.verticalScroll(rememberScrollState())) {
            accounts.forEach { account ->
                GreenAccountAsset(
                    accountAssetBalance = account.accountAssetBalance,
                    session = viewModel.sessionOrNull,
                ) {
                    viewModel.selectAccount(account)
                }
            }
        }
    }
}

@Composable
@Preview
fun ReceiveChooseAccountScreenPreview() {
    GreenPreview {
        ReceiveChooseAccountScreen(viewModel = ReceiveChooseAccountViewModelPreview.preview())
    }
}

