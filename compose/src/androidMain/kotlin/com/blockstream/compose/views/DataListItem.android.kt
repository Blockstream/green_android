package com.blockstream.compose.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.utils.StringHolder
import com.blockstream.ui.components.GreenColumn
import com.blockstream.compose.theme.GreenChromePreview

@Preview
@Composable
fun DataListItemPreview() {
    GreenChromePreview {
        GreenColumn {
            DataListItem(
                StringHolder.create("Title"),
                StringHolder.create("DataListItem"),
                withDivider = true
            )
            DataListItem(
                StringHolder.create("Title"),
                StringHolder.create("withDivider = true"),
                withDivider = true
            )
            DataListItem(
                StringHolder.create("Title"),
                StringHolder.create("withDivider = false"),
                withDivider = false
            )
            DataListItem(
                StringHolder.create("Title"),
                StringHolder.create("DataListItem"),
                withDivider = true,
                withDataLayout = true
            )
        }
    }
}