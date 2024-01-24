package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.blockstream.compose.R
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.navigation.setNavigationResult


object EnvironmentBottomSheet : BottomScreen() {
    @Composable
    override fun Content() {
        MenuBottomSheetView(title = stringResource(R.string.id_select_network), entries = listOf(
            MenuEntry(title = "Mainnet", iconRes = R.drawable.currency_btc) {
                setNavigationResult(resultKey, false)
            },
            MenuEntry(title = "Testnet", iconRes = R.drawable.flask) {
                setNavigationResult(resultKey, true)
            }
        ), onDismissRequest = onDismissRequest())
    }
}