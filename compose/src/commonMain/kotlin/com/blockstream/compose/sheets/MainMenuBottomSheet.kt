package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_account_transfer
import blockstream_green.common.generated.resources.id_buy
import blockstream_green.common.generated.resources.id_move_across_accounts
import blockstream_green.common.generated.resources.id_qr_scanner
import blockstream_green.common.generated.resources.id_redeposit
import blockstream_green.common.generated.resources.id_redeposit_expired_2fa_coins
import blockstream_green.common.generated.resources.id_scan_qr_code
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowULeftDown
import com.adamglin.phosphoricons.regular.ArrowsDownUp
import com.adamglin.phosphoricons.regular.Coins
import com.adamglin.phosphoricons.regular.QrCode
import com.blockstream.common.CountlyBase
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.compose.LocalAppInfo
import com.blockstream.compose.components.GreenArrow
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.setNavigationResult
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteLow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

enum class MainMenuEntry {
    BUY_SELL, ACCOUNT_TRANSFER, SCAN, REDEPOSIT;
}

@Parcelize
data class MainMenuBottomSheet(val isTestnet: Boolean) : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {
        val onDismissRequest = onDismissRequest()
        MainMenuBottomSheetView(
            onSelect = { menuEntry ->
                setResult(menuEntry)
                onDismissRequest.invoke()
            },
            isTestnet = isTestnet,
            onDismissRequest = onDismissRequest
        )
    }

    companion object{
        @Composable
        fun getResult(fn: (MainMenuEntry) -> Unit) =
            getNavigationResult(this::class, fn)

        private fun setResult(result: MainMenuEntry) =
            setNavigationResult(this::class, result)
    }
}

@Composable
fun MainMenuItem(title: String, subtitle: String, icon: ImageVector, onClick: (() -> Unit)? = null) {
    GreenCard(onClick = onClick) {
        GreenRow(padding = 0, space = 16) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = titleSmall)
                Text(subtitle, color = whiteLow)
            }
            GreenArrow()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuBottomSheetView(
    onSelect: (item: MainMenuEntry) -> Unit,
    isTestnet: Boolean,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        withHorizontalPadding = false,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
        viewModel = null,
        onDismissRequest = {
            onDismissRequest.invoke()
        }
    ) {

        val appInfo = LocalAppInfo.current
        val countly = koinInject<CountlyBase>()


        GreenColumn {
            if(countly.getRemoteConfigForOnOffRamps() != false && !isTestnet) {
                MainMenuItem(
                    title = stringResource(Res.string.id_buy),
                    subtitle = "BTC",
                    icon = PhosphorIcons.Regular.Coins,
                    onClick = {
                        countly.buyInitiate()
                        onSelect(MainMenuEntry.BUY_SELL)
                    }
                )
            }

             if(appInfo.isDevelopment) {
                 MainMenuItem(
                     title = stringResource(Res.string.id_account_transfer),
                     subtitle = stringResource(
                         Res.string.id_move_across_accounts
                     ),
                     icon = PhosphorIcons.Regular.ArrowsDownUp,
                     onClick = {
                         onSelect(MainMenuEntry.ACCOUNT_TRANSFER)
                     }
                 )
                 MainMenuItem(
                     title = stringResource(Res.string.id_redeposit),
                     subtitle = stringResource(Res.string.id_redeposit_expired_2fa_coins),
                     icon = PhosphorIcons.Regular.ArrowULeftDown, onClick = {
                         onSelect(MainMenuEntry.REDEPOSIT)
                     }
                 )
             }

            MainMenuItem(
                title = stringResource(Res.string.id_qr_scanner),
                subtitle = stringResource(Res.string.id_scan_qr_code),
                icon = PhosphorIcons.Regular.QrCode, onClick = {
                    onSelect(MainMenuEntry.SCAN)
                }
            )
        }
    }
}