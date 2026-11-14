package com.blockstream.compose.sheets

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.arrow_square_out
import blockstream_green.common.generated.resources.id_coin_details
import blockstream_green.common.generated.resources.id_received_on
import blockstream_green.common.generated.resources.id_transaction_id
import blockstream_green.common.generated.resources.id_view_in_explorer
import com.blockstream.compose.components.GreenBottomSheet
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.extensions.colorTextEdges
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.models.sheets.CoinInfoViewModelPreview
import com.blockstream.compose.models.sheets.CoinInfoViewModelAbstract
import com.blockstream.compose.theme.MonospaceFont
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.StringHolder
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CoinInfoBottomSheet(
    viewModel: CoinInfoViewModelAbstract,
    onDismissRequest: () -> Unit
) {
    GreenBottomSheet(
        title = stringResource(Res.string.id_coin_details),
        titleTextAlign = TextAlign.Start,
        viewModel = viewModel,
        onDismissRequest = onDismissRequest
    ) {
        val data by viewModel.data.collectAsStateWithLifecycle()
        val onProgress by viewModel.onProgress.collectAsStateWithLifecycle()

        val density = LocalDensity.current
        val windowInfo = LocalWindowInfo.current
        val maxContentHeight = with(density) { (windowInfo.containerSize.height * 0.45f).toDp() }

        AnimatedContent(
            targetState = data,
            label = "CoinInfoData"
        ) { items ->
            Column(
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 8.dp)
                    .heightIn(max = maxContentHeight)
                    .verticalScroll(rememberScrollState())
            ) {
                items.forEach { pair ->
                    CoinInfoDataListItem(
                        title = pair.first,
                        data = pair.second
                    )
                }
            }
        }

        if (onProgress) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(color = green, strokeWidth = 2.dp)
            }
        }

        TextButton(
            onClick = {
                viewModel.postEvent(CoinInfoViewModelAbstract.LocalEvents.ViewInExplorer)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(
                text = stringResource(Res.string.id_view_in_explorer),
                style = bodyLarge,
                color = green,
                textDecoration = TextDecoration.Underline
            )
            Icon(
                painter = painterResource(Res.drawable.arrow_square_out),
                contentDescription = null,
                tint = green,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
private fun CoinInfoDataListItem(
    title: StringHolder,
    data: StringHolder
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = title.string(),
            style = bodyMedium,
            color = whiteMedium,
            modifier = Modifier.weight(1f)
        )
        when (title.stringResource) {
            Res.string.id_received_on -> {
                val address = data.string()
                val platformManager = LocalPlatformManager.current
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier.weight(1.15f),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Text(
                        text = colorTextEdges(
                            text = address.chunked(4).joinToString(" "),
                            numberOfSections = 2
                        ),
                        style = bodySmall,
                        fontFamily = MonospaceFont(),
                        textAlign = TextAlign.End,
                        modifier = Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            platformManager.copyToClipboard(content = address)
                        }
                    )
                }
            }

            Res.string.id_transaction_id -> {
                val transactionId = data.string()
                val platformManager = LocalPlatformManager.current
                val interactionSource = remember { MutableInteractionSource() }
                Text(
                    text = transactionId.shortMiddle(),
                    style = bodySmall,
                    color = whiteHigh,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .weight(1.15f)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            platformManager.copyToClipboard(content = transactionId)
                        }
                )
            }

            else -> {
                Text(
                    text = data.string(),
                    style = bodySmall,
                    color = whiteHigh,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.15f)
                )
            }
        }
    }
}

private fun String.shortMiddle(): String =
    if (length > 18) "${take(9)}...${takeLast(9)}" else this

@Preview
@Composable
fun CoinInfoBottomSheetPreview() {
    GreenPreview {
        CoinInfoBottomSheet(
            viewModel = CoinInfoViewModelPreview(),
            onDismissRequest = {}
        )
    }
}
