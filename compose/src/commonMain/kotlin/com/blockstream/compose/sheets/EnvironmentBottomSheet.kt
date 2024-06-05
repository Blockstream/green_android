package com.blockstream.compose.sheets

import androidx.compose.runtime.Composable
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_select_network
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.setNavigationResult
import org.jetbrains.compose.resources.stringResource


object EnvironmentBottomSheet : BottomScreen() {
    @Composable
    override fun Content() {
        MenuBottomSheetView(
            title = stringResource(Res.string.id_select_network), entries = listOf(
                MenuEntry(title = "Mainnet", iconRes = "currency_btc"),
                MenuEntry(title = "Testnet", iconRes = "flask")
            ), onSelect = { position, menuEntry ->
                setResult(position != 0)
            }, onDismissRequest = onDismissRequest()
        )
    }

    @Composable
    fun getResult(fn: (Boolean) -> Unit) =
        getNavigationResult(this::class, fn)

    private fun setResult(result: Boolean) =
        setNavigationResult(this::class, result)
}