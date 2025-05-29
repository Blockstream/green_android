package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_select_network
import com.blockstream.common.data.MenuEntry
import com.blockstream.common.data.MenuEntryList
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.ui.navigation.setResult
import org.jetbrains.compose.resources.stringResource

@Composable
fun EnvironmentBottomSheet(onDismissRequest: () -> Unit) {
    var resultFromUserSelection by remember { mutableStateOf(false) }

    MenuBottomSheetView(
        title = stringResource(Res.string.id_select_network), entries = MenuEntryList(
            listOf(
                MenuEntry(title = "Mainnet", iconRes = "currency_btc"),
                MenuEntry(title = "Testnet", iconRes = "flask")
            )
        ), onSelect = { position, _ ->
            NavigateDestinations.Environment.setResult(position)

            resultFromUserSelection = true
        }, onDismissRequest = {
            if (!resultFromUserSelection) {
                NavigateDestinations.Environment.setResult(-1)
            }
            onDismissRequest()
        }
    )
}