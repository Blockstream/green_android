package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import com.blockstream.common.data.MenuEntry
import com.blockstream.common.data.MenuEntryList
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.extensions.toDrawableResource
import org.jetbrains.compose.resources.painterResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuBottomSheetView(
    title: String,
    subtitle: String? = null,
    entries: MenuEntryList,
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
            entries.list.forEachIndexed { index, menuEntry ->
                DropdownMenuItem(
                    text = { Text(menuEntry.title) },
                    onClick = {
                        onSelect.invoke(index, menuEntry)
                        onDismissRequest.invoke()
                    },
                    leadingIcon = menuEntry.iconRes?.toDrawableResource()?.let {
                        {
                            Icon(
                                painterResource(it),
                                contentDescription = null
                            )
                        }
                    }
                )
            }
        }
    }
}