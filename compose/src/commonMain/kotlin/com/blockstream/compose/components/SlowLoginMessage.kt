package com.blockstream.compose.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_login_taking_longer_than_usual
import blockstream_green.common.generated.resources.id_more_information
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowSquareOut
import com.blockstream.data.Urls
import com.blockstream.compose.theme.titleLarge
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.noRippleClickable
import org.jetbrains.compose.resources.stringResource

@Composable
fun SlowLoginMessage(
    onClickStatusPage: () -> Unit
) {
    GreenColumn(
        padding = 24,
        space = 8,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.id_login_taking_longer_than_usual),
            style = titleLarge,
            color = whiteHigh,
            textAlign = TextAlign.Center
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.id_more_information),
                style = MaterialTheme.typography.bodyLarge,
                color = whiteMedium
            )

            Row(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .sizeIn(minHeight = 48.dp)
                    .semantics { role = Role.Button }
                    .noRippleClickable(onClick = onClickStatusPage),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Urls.STATUS_PAGE.removePrefix("https://"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    textDecoration = TextDecoration.Underline
                )
                Icon(
                    imageVector = PhosphorIcons.Regular.ArrowSquareOut,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(16.dp)
                )
            }
        }
    }
}
