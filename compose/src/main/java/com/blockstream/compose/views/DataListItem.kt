package com.blockstream.compose.views

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.copyToClipboard
import com.blockstream.compose.utils.noRippleClickable
import com.blockstream.compose.utils.stringResourceId

@Composable
fun DataListItem(
    title: String,
    data: String,
    withDivider: Boolean = true,
) {
    GreenRow(padding = 0, space = 8) {
        GreenColumn(padding = 0, space = 2, modifier = Modifier.weight(1f)) {
            Text(
                text = stringResourceId(id = title),
                style = labelLarge,
                color = whiteHigh
            )
            Text(text = data, style = bodyLarge, color = whiteMedium)
        }

        val context = LocalContext.current
        Icon(
            painter = painterResource(id = R.drawable.copy),
            contentDescription = "Copy",
            tint = whiteLow,
            modifier = Modifier.noRippleClickable {
                copyToClipboard(context = context, "Green", content = data)
            }
        )
    }
    if (withDivider) {
        HorizontalDivider()
    }
}

@Preview
@Composable
fun DataListItemPreview() {
    GreenThemePreview {
        GreenColumn {
            DataListItem("Title", "DataListItem")
            DataListItem("Title", "withDivider = true")
            DataListItem("Title", "withDivider = false", withDivider = false)
        }
    }
}