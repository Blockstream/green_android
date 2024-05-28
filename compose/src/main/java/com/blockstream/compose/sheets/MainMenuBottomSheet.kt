package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenArrow
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenCard
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.navigation.setNavigationResult
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteLow

enum class MainMenuEntry {
    ACCOUNT_TRANSFER, SCAN, REDEPOSIT;
}

object MainMenuBottomSheet : BottomScreen() {
    @Composable
    override fun Content() {
        val onDismissRequest = onDismissRequest()
        MainMenuBottomSheetView(
            onSelect = { menuEntry ->
                setNavigationResult(resultKey, menuEntry)
                onDismissRequest.invoke()
            },
            onDismissRequest = onDismissRequest
        )
    }
}

@Composable
fun MainMenuItem(title: String, subtitle: String, icon: Painter, onClick: (() -> Unit)? = null) {
    GreenCard(onClick = onClick) {
        GreenRow(padding = 0, space = 16) {
            Icon(
                painter = icon,
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

        GreenColumn {
            MainMenuItem(
                title = stringResource(R.string.id_account_transfer),
                subtitle = stringResource(
                    R.string.id_move_across_accounts
                ),
                icon = painterResource(R.drawable.arrows_down_up),
                onClick = {
                    onSelect(MainMenuEntry.ACCOUNT_TRANSFER)
                }
            )
            MainMenuItem(
                title = stringResource(R.string.id_redeposit),
                subtitle = stringResource(R.string.id_redeposit_expired_2fa_coins),
                icon = painterResource(R.drawable.arrow_u_left_down), onClick = {
                    onSelect(MainMenuEntry.REDEPOSIT)
                }
            )
            MainMenuItem(
                title = stringResource(R.string.id_scan_qr_code),
                subtitle = stringResource(R.string.id_scan_a_proposal),
                icon = painterResource(R.drawable.qr_code), onClick = {
                    onSelect(MainMenuEntry.SCAN)
                }
            )
        }
    }
}

@Composable
@Preview
fun MainMenuBottomSheetPreview() {
    GreenPreview {
        GreenColumn {
            var showBottomSheet by remember { mutableStateOf(true) }

            GreenButton(text = "Show BottomSheet") {
                showBottomSheet = true
            }

            var environment by remember { mutableStateOf("-") }
            Text("MenuBottomSheetPreview env: $environment")

            if (showBottomSheet) {
                MainMenuBottomSheetView(
                    onSelect = { menuEntry ->

                    },
                    onDismissRequest = {
                        showBottomSheet = false
                    }
                )
            }
        }
    }
}