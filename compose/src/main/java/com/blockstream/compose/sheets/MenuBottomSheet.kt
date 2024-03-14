package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.navigation.setNavigationResult
import com.blockstream.compose.components.GreenBottomSheet


@Parcelize
data class MenuEntry(
    val key: Int = 0,
    val title: String,
    val iconRes: Int? = null
) : Parcelable

@Parcelize
data class MenuBottomSheet(
    val title: String,
    val subtitle: String? = null,
    val entries: List<MenuEntry>
) : BottomScreen(), Parcelable {
    @Composable
    override fun Content() {
        MenuBottomSheetView(
            title = title,
            subtitle = subtitle,
            entries = entries,
            onSelect = { position, menuEntry ->
                setNavigationResult(resultKey, position)
            },
            onDismissRequest = onDismissRequest()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuBottomSheetView(
    title: String,
    subtitle: String? = null,
    entries: List<MenuEntry>,
    onSelect: (position: Int, item: MenuEntry) -> Unit,
    onDismissRequest: () -> Unit,
) {
    GreenBottomSheet(
        title = title,
        subtitle = subtitle,
        withHorizontalPadding = false,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
        viewModel = null,
        onDismissRequest = {
            onDismissRequest.invoke()
        }
    ) {

        Column {
            entries.forEachIndexed { index, menuEntry ->
                DropdownMenuItem(
                    text = { Text(menuEntry.title) },
                    onClick = {
                        onSelect.invoke(index, menuEntry)
                        onDismissRequest.invoke()
                    },
                    leadingIcon = menuEntry.iconRes?.let {
                        {
                            Icon(
                                painterResource(id = it),
                                contentDescription = null
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
@Preview
fun MenuBottomSheetPreview() {
    GreenPreview {
        GreenColumn {
            var showBottomSheet by remember { mutableStateOf(true) }

            GreenButton(text = "Show BottomSheet") {
                showBottomSheet = true
            }

            var environment by remember { mutableStateOf("-") }
            Text("MenuBottomSheetPreview env: $environment")

            if (showBottomSheet) {
                MenuBottomSheetView(
                    title = "Select Environment",
                    entries = listOf(
                        MenuEntry(title = "Mainnet", iconRes = R.drawable.currency_btc),
                        MenuEntry(title = "Testnet", iconRes = R.drawable.flask)
                    ),
                    onSelect = { position, menuEntry ->

                    },
                    onDismissRequest = {
                        showBottomSheet = false
                    }
                )
            }
        }
    }
}