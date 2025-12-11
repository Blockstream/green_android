package com.blockstream.compose.views

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Copy
import com.blockstream.common.utils.StringHolder
import com.blockstream.compose.components.GreenDataLayout
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.noRippleClickable
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import kotlinx.coroutines.launch

@Composable
fun DataListItem(
    title: StringHolder,
    data: StringHolder,
    withDivider: Boolean = false,
    withDataLayout: Boolean = false,
) {
    val platformManager = LocalPlatformManager.current
    val scope = rememberCoroutineScope()

    if (withDataLayout) {
        GreenDataLayout(title = title.string(), withPadding = false) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = data.string(),
                    style = bodyLarge,
                    modifier = Modifier
                        .padding(16.dp)
                        .weight(1f)
                )

                IconButton(
                    onClick = {
                        scope.launch {
                            platformManager.copyToClipboard(content = data.getString())
                        }
                    }
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Copy,
                        contentDescription = null,
                    )
                }
            }
        }
    } else {
        GreenRow(padding = 0, space = 8) {
            GreenColumn(padding = 0, space = 2, modifier = Modifier.weight(1f)) {
                Text(
                    text = title.string(),
                    style = labelLarge,
                    color = whiteHigh
                )
                Text(text = data.string(), style = bodyLarge, color = whiteMedium)
            }

            Icon(
                imageVector = PhosphorIcons.Regular.Copy,
                contentDescription = "Copy",
                tint = whiteLow,
                modifier = Modifier.noRippleClickable {
                    scope.launch {
                        platformManager.copyToClipboard(content = data.getString())
                    }
                }
            )
        }

        if (withDivider) {
            HorizontalDivider()
        }
    }
}
