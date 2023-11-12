package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.compose.R


@Parcelize
class EnvironmentBottomSheet(val onEnvironment: (isTestnet: Boolean?) -> Unit) : BottomScreen() {
    @Composable
    override fun Content() {
        MenuBottomSheetView(title = stringResource(R.string.id_select_network), entries = listOf(
            MenuEntry(title = "Mainnet", iconRes = R.drawable.currency_btc) {
                onEnvironment.invoke(true)
            },
            MenuEntry(title = "Testnet", iconRes = R.drawable.flask) {
                onEnvironment.invoke(false)
            }
        ), onDismissRequest = onDismissRequest())
    }
}