package com.blockstream.compose.screens.send

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockstream.common.models.send.SendChooseAccountViewModelAbstract
import com.blockstream.common.models.send.SendChooseAccountViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenAccountAsset
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.ui.components.GreenColumn
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SendChooseAccountScreen(
    viewModel: SendChooseAccountViewModelAbstract
) {

    SetupScreen(
        viewModel = viewModel
    ) {
        val accounts by viewModel.accountsState.collectAsStateWithLifecycle()

        GreenColumn(padding = 0, space = 4, modifier = Modifier.verticalScroll(rememberScrollState())) {
            accounts.forEach { accountState ->
                GreenAccountAsset(
                    accountAssetBalance = accountState.account,
                    session = viewModel.sessionOrNull,
                    helperText = accountState.message,
                    isError = accountState.isError,
                    onClick = {
                        viewModel.selectAccount(accountState.account)
                    }.takeIf { !accountState.isError }
                )
            }
        }
    }
}

@Composable
@Preview
fun SendChooseAccountScreenPreview() {
    GreenPreview {
        SendChooseAccountScreen(viewModel = SendChooseAccountViewModelPreview.preview())
    }
}

