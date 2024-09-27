package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_select_network
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.setNavigationResult
import org.jetbrains.compose.resources.stringResource


object EnvironmentBottomSheet : BottomScreen() {

    @Composable
    override fun Content() {
        var resultFromUserSelection by remember { mutableStateOf(false) }

        MenuBottomSheetView(
            title = stringResource(Res.string.id_select_network), entries = listOf(
                MenuEntry(title = "Mainnet", iconRes = "currency_btc"),
                MenuEntry(title = "Testnet", iconRes = "flask")
            ), onSelect = { position, _ ->
                setResult(position)
                resultFromUserSelection = true
            }, onDismissRequest = onDismissRequest {
                if(!resultFromUserSelection){
                    setResult(-1)
                }
            }
        )
    }

    @Composable
    fun getResult(fn: (Int) -> Unit) =
        getNavigationResult(this::class, fn)

    private fun setResult(result: Int) =
        setNavigationResult(this::class, result)
}