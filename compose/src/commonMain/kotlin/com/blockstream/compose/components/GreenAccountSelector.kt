package com.blockstream.compose.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_account
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.fill.CaretDown
import com.blockstream.common.gdk.data.Account
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.noRippleClickable
import com.blockstream.ui.components.GreenRow
import com.blockstream.ui.utils.appTestTag
import org.jetbrains.compose.resources.stringResource

@Composable
fun GreenAccountSelector(
    modifier: Modifier = Modifier,
    account: Account,
    onClick: (() -> Unit) = {},
) {
    GreenRow(
        padding = 0,
        modifier = modifier.appTestTag("account_selector")
    ) {
        Text(
            text = stringResource(Res.string.id_account),
            style = bodyLarge,
            color = whiteMedium
        )

        GreenRow(space = 8, padding = 0, modifier = Modifier.noRippleClickable {
            onClick.invoke()
        }.align(Alignment.CenterVertically)) {
            Text(
                text = account.name,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = PhosphorIcons.Fill.CaretDown,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
