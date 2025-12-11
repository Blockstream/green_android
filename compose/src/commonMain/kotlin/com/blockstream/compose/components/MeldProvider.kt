package com.blockstream.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_no_quotes_available_for_this_amount
import blockstream_green.common.generated.resources.id_you_receive
import blockstream_green.common.generated.resources.pencil_simple_line
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.green.data.meld.data.QuoteResponse
import com.blockstream.compose.utils.ifTrue
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun MeldProvider(
    modifier: Modifier = Modifier,
    quote: QuoteResponse?,
    title: String? = null,
    badge: String? = null,
    error: String? = null,
    onProgress: Boolean,
    isChecked: Boolean = false,
    withEditIcon: Boolean = false,
    onClick: (() -> Unit) = {},
) {
    GreenDataLayout(
        modifier = modifier,
        title = title,
        badge = badge,
        withPadding = false,
        border = BorderStroke(1.dp, green).takeIf { isChecked },
        enabled = !onProgress,
        testTag = "exchange_" + quote?.serviceProvider,
        onClick = onClick
    ) {

        Box {
            if (!onProgress && (quote == null || error != null)) {
                Text(
                    error ?: stringResource(Res.string.id_no_quotes_available_for_this_amount),
                    color = whiteMedium,
                    style = bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 16.dp)
                    .ifTrue(!withEditIcon) {
                        it.padding(end = 16.dp)
                    }
                    .ifTrue(onProgress || quote == null) {
                        it.alpha(0f)
                    }
            ) {
                Text(
                    text = quote?.serviceProvider ?: "",
                    style = labelLarge,
                    modifier = Modifier.weight(1f).padding(vertical = 16.dp)
                )

                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(vertical = 16.dp)) {
                    Text(
                        text = "${quote?.destinationAmount} ${quote?.destinationCurrencyCode}".trim(),
                        style = titleSmall,
                        textAlign = TextAlign.End
                    )

                    Text(
                        text = stringResource(Res.string.id_you_receive),
                        style = bodySmall,
                        textAlign = TextAlign.End,
                        color = whiteMedium
                    )
                }

                if (withEditIcon) {
                    IconButton(onClick = onClick) {
                        Icon(
                            painter = painterResource(Res.drawable.pencil_simple_line),
                            contentDescription = "Edit",
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }
            }

            if (onProgress) {
                CircularProgressIndicator(
                    strokeWidth = 1.dp,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
            }
        }
    }
}
