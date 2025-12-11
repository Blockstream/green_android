package com.blockstream.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.arrow_down_left
import blockstream_green.common.generated.resources.arrow_up_right
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.gdk.GdkSession
import com.blockstream.common.looks.AmountAssetLook
import com.blockstream.common.utils.DecimalFormat
import com.blockstream.compose.extensions.assetIcon
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.noRippleClickable
import org.jetbrains.compose.resources.painterResource

@Composable
fun GreenAmounts(
    modifier: Modifier = Modifier,
    amounts: List<AmountAssetLook>,
    session: GdkSession? = null,
    onAssetClick: ((assetId: String) -> Unit) = { _ -> }
) {
    if (amounts.size == 1) {
        val look = amounts.first()
        val decimalSymbol = remember { DecimalFormat.DecimalSeparator }

        val (integer, fractional) = look.amount.split(decimalSymbol).let {
            (it.firstOrNull() ?: "") to (it.getOrNull(1) ?: "")
        }

        val annotatedAmount = buildAnnotatedString {
            withStyle(style = SpanStyle(fontSize = 32.sp)) {
                append(integer)
            }
            if (fractional.isNotBlank()) {
                withStyle(style = SpanStyle(fontSize = 22.sp)) {
                    append(decimalSymbol)
                    append(fractional.chunked(3).joinToString(" "))
                }
            }
            withStyle(style = SpanStyle(fontSize = 16.sp, color = green)) {
                append(" ${look.ticker}")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(modifier),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            SelectionContainer {
                Text(
                    text = annotatedAmount,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.noRippleClickable {
                        onAssetClick.invoke(look.assetId)
                    }
                )
            }

            look.fiat?.also { value ->
                SelectionContainer {
                    Text(
                        text = value,
                        style = bodyLarge,
                        color = whiteMedium
                    )
                }
            }

        }
    } else if (amounts.isNotEmpty()) {
        GreenColumn(
            padding = 0,
            space = 4,
            modifier = Modifier
                .fillMaxWidth()
                .then(modifier)
        ) {

            amounts.forEach {
                GreenRow(
                    padding = 0,
                    space = 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onAssetClick.invoke(it.assetId)
                        }) {
                    Box {

                        Image(
                            painter = it.assetId.assetIcon(session = session),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .padding(end = 18.dp)
                                .padding(vertical = 8.dp)
                                .size(32.dp)

                        )

                        Image(
                            painter = painterResource(if (it.isOutgoing) Res.drawable.arrow_up_right else Res.drawable.arrow_down_left),
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }

                    Column {
                        GreenRow(padding = 0, space = 4) {

                            val annotatedAmount = buildAnnotatedString {
                                withStyle(style = SpanStyle(fontSize = 24.sp)) {
                                    append(it.amount)
                                }
                                withStyle(style = SpanStyle(fontSize = 16.sp, color = green)) {
                                    append(" ${it.ticker}")
                                }
                            }

                            SelectionContainer(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = annotatedAmount,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 24.sp,
                                    modifier = Modifier
                                        .alignByBaseline(),
                                    textAlign = TextAlign.End
                                )
                            }
                        }

                        it.fiat?.also { fiat ->
                            GreenRow(padding = 0, space = 4) {
                                SelectionContainer(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fiat,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = bodyLarge,
                                        modifier = Modifier
                                            .weight(1f),
                                        textAlign = TextAlign.End,
                                        color = whiteMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}