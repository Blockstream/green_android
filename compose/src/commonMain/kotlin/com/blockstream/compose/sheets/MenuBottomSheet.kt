package com.blockstream.compose.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.extensions.toDrawableResource
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.setNavigationResult
import org.jetbrains.compose.resources.painterResource


@Parcelize
data class MenuEntry constructor(
    val key: Int = 0,
    val title: String,
    val iconRes: String? = null
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
                setResult(position)
            },
            onDismissRequest = onDismissRequest()
        )
    }

    companion object {
        @Composable
        fun getResult(fn: (Int) -> Unit) =
            getNavigationResult(this::class, fn)

        internal fun setResult(result: Int) =
            setNavigationResult(this::class, result)
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