package com.blockstream.compose.screens.lightning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_amount_to_receive
import blockstream_green.common.generated.resources.id_copy_invoice
import blockstream_green.common.generated.resources.id_enlarge_qr
import blockstream_green.common.generated.resources.id_expires_s
import blockstream_green.common.generated.resources.id_fee
import blockstream_green.common.generated.resources.id_funding_fee
import blockstream_green.common.generated.resources.id_invoice
import blockstream_green.common.generated.resources.id_note
import blockstream_green.common.generated.resources.id_qr_code
import blockstream_green.common.generated.resources.id_share
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Copy
import com.adamglin.phosphoricons.regular.Info
import com.adamglin.phosphoricons.regular.MagnifyingGlassPlus
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.BorderedQrProps
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenDataLayout
import com.blockstream.compose.components.GreenQR
import com.blockstream.compose.components.OnProgressStyle
import com.blockstream.compose.components.QrBorderConfig
import com.blockstream.compose.managers.rememberPlatformManager
import com.blockstream.compose.models.lightning.LightningInvoiceViewModel.LocalEvents
import com.blockstream.compose.models.lightning.LightningInvoiceViewModelAbstract
import com.blockstream.compose.models.lightning.LightningInvoiceViewModelPreview
import com.blockstream.compose.navigation.NavigateDestinations
import com.blockstream.compose.navigation.getResult
import com.blockstream.compose.theme.MonospaceFont
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.lightning
import com.blockstream.compose.theme.whiteHigh
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.CopyContainer
import com.blockstream.compose.utils.SetupScreen
import com.blockstream.data.data.MenuEntry
import com.blockstream.data.data.MenuEntryList
import io.github.alexzhirkevich.qrose.QrCodePainter
import io.github.alexzhirkevich.qrose.toByteArray
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun LightningInvoiceScreen(
    viewModel: LightningInvoiceViewModelAbstract
) {
    val scope = rememberCoroutineScope()
    val platformManager = rememberPlatformManager()

    val data = viewModel.state

    NavigateDestinations.LightningFeeInfo.getResult<Boolean> { confirmed ->
        if (confirmed) {
            viewModel.postEvent(LocalEvents.ClickFundingFeeLearnMore)
        }
    }

    NavigateDestinations.Menu.getResult<Int> { index ->
        if (index == 0) {
            viewModel.postEvent(LocalEvents.ShareAddress)
        } else {
            scope.launch {
                runCatching {
                    val qrCode: Painter = QrCodePainter(data = data.invoiceUri)
                    val processedData = qrCode.toByteArray(800, 800).let { bytes ->
                        platformManager.processQr(bytes, data.invoiceUri)
                    }
                    viewModel.postEvent(LocalEvents.ShareQR(processedData))
                }
            }
        }
    }

    SetupScreen(
        viewModel = viewModel,
        withPadding = false,
        withImePadding = true,
        onProgressStyle = OnProgressStyle.Disabled,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {

                data.expiration?.also { exp ->
                    if (exp.isNotBlank()) {
                        Text(
                            text = stringResource(Res.string.id_expires_s, exp),
                            style = bodySmall,
                            color = whiteMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    }
                }

                GreenQR(
                    modifier = Modifier.fillMaxWidth(),
                    data = data.invoiceUri,
                    borderedProps = BorderedQrProps(
                        config = QrBorderConfig(
                            color = lightning,
                        ),
                        footer = { openFullScreen ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {

                                Box(
                                    modifier = Modifier
                                        .width(215.dp)
                                        .padding(top = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LightningInvoiceAddress(
                                        address = data.invoiceUri,
                                        onCopyClick = { viewModel.postEvent(LocalEvents.CopyAddress) }
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    GreenButton(
                                        text = stringResource(Res.string.id_enlarge_qr),
                                        icon = PhosphorIcons.Regular.MagnifyingGlassPlus,
                                        type = GreenButtonType.OUTLINE,
                                        color = GreenButtonColor.GREENER,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        openFullScreen()
                                    }

                                    GreenButton(
                                        text = stringResource(Res.string.id_copy_invoice),
                                        icon = PhosphorIcons.Regular.Copy,
                                        type = GreenButtonType.OUTLINE,
                                        color = GreenButtonColor.GREENER,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        viewModel.postEvent(LocalEvents.CopyAddress)
                                    }
                                }
                            }
                        }
                    )
                )

                data.description?.also { desc ->
                    if (desc.isNotBlank()) {
                        GreenDataLayout(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 12.dp),
                            title = stringResource(Res.string.id_note),
                            withPadding = true
                        ) {
                            Text(
                                text = desc,
                                style = bodyMedium,
                                color = whiteHigh,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    data.feeText?.also { feeText ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (!data.isSwap) {
                                            Modifier.clickable {
                                                viewModel.postEvent(LocalEvents.ClickFundingFee)
                                            }
                                        } else Modifier
                                    )
                            ) {
                                Text(
                                    text = if (data.isSwap) {
                                        stringResource(Res.string.id_fee)
                                    } else {
                                        stringResource(Res.string.id_funding_fee)
                                    },
                                    style = bodyMedium,
                                    color = whiteMedium
                                )

                                if (!data.isSwap) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = PhosphorIcons.Regular.Info,
                                        contentDescription = "Info",
                                        tint = whiteMedium,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = feeText, style = bodyMedium, color = whiteMedium)
                                data.feeFiatText?.also { feeFiat ->
                                    Text(text = "≈ $feeFiat", style = bodySmall, color = whiteLow)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = stringResource(Res.string.id_amount_to_receive),
                            style = labelMedium,
                            color = whiteHigh,
                            modifier = Modifier.weight(1f)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = data.amount, style = labelMedium, color = whiteHigh)
                            data.amountFiat?.also { fiat ->
                                Text(text = "≈ $fiat", style = bodySmall, color = whiteLow)
                            }
                        }
                    }
                }
            }

            GreenButton(
                size = GreenButtonSize.BIG,
                text = stringResource(Res.string.id_share),
                color = GreenButtonColor.GREEN,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                scope.launch {
                    viewModel.postEvent(
                        NavigateDestinations.Menu(
                            title = getString(Res.string.id_share),
                            entries = MenuEntryList(
                                listOf(
                                    MenuEntry(
                                        title = getString(Res.string.id_invoice),
                                        iconRes = "text-aa"
                                    ),
                                    MenuEntry(
                                        title = getString(Res.string.id_qr_code),
                                        iconRes = "qr-code"
                                    ),
                                )
                            )
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LightningInvoiceAddress(
    address: String,
    modifier: Modifier = Modifier,
    onCopyClick: ((String) -> Unit)? = null
) {
    val clean = address.substringAfter(":")
    val chunks = clean.chunked(4)

    val content = @Composable {
        if (chunks.size >= 8) {
            val c1 = chunks[0]
            val c2 = chunks[1]
            val c3 = chunks[2]
            val c4 = chunks[3]

            val cLast4 = chunks[chunks.size - 4]
            val cLast3 = chunks[chunks.size - 3]
            val cLast2 = chunks[chunks.size - 2]
            val cLast1 = chunks[chunks.size - 1]

            val row1Text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = lightning)) {
                    append("$c1  $c2")
                }
                withStyle(style = SpanStyle(color = whiteHigh)) {
                    append("  $c3  $c4")
                }
            }

            val row3Text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = whiteHigh)) {
                    append("$cLast4  $cLast3  ")
                }
                withStyle(style = SpanStyle(color = lightning)) {
                    append("$cLast2  $cLast1")
                }
            }

            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = row1Text,
                    fontFamily = MonospaceFont(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "...",
                    fontFamily = MonospaceFont(),
                    color = whiteLow,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                Text(
                    text = row3Text,
                    fontFamily = MonospaceFont(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Text(
                text = clean,
                fontFamily = MonospaceFont(),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (onCopyClick == null) {
        CopyContainer(value = address, withSelection = false) {
            content()
        }
    } else {
        Box(modifier = Modifier.clickable { onCopyClick(address) }) {
            content()
        }
    }
}

@Composable
@Preview
fun LightningInvoiceScreenPreview() {
    GreenPreview {
        LightningInvoiceScreen(
            viewModel = LightningInvoiceViewModelPreview()
        )
    }
}