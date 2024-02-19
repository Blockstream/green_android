package com.blockstream.compose.screens.transaction

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.FixedScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockstream.common.looks.transaction.Completed
import com.blockstream.common.looks.transaction.Failed
import com.blockstream.common.looks.transaction.TransactionStatus
import com.blockstream.common.looks.transaction.Unconfirmed
import com.blockstream.compose.R
import com.blockstream.compose.extensions.color
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.utils.Rotating
import kotlin.math.max

@Composable
fun TransactionStatusIcon(
    modifier: Modifier = Modifier,
    transactionStatus: TransactionStatus,
    icons: List<Painter>,
    isSwap: Boolean = false,
) {
    val icon = when (transactionStatus) {
        is Failed -> R.drawable.x_bold
        is Unconfirmed -> R.drawable.arrows_counter_clockwise_bold
        else -> R.drawable.check_bold
    }

    val color = transactionStatus.color()

    val assetSize = 34.dp

    Box(modifier = modifier) {
        if (transactionStatus is Unconfirmed) {
            Rotating(duration = 2000) {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    contentScale = FixedScale(1.5f),
                    modifier = Modifier
                        .padding(24.dp)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        } else {
            Image(
                painter = painterResource(id = icon),
                contentDescription = null,
                contentScale = FixedScale(1.5f),
                modifier = Modifier
                    .padding(24.dp)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }


        if (isSwap) {
            Image(
                painter = painterResource(id = R.drawable.arrow_bend_left_down),
                contentDescription = null,
                modifier = Modifier
                    .padding(bottom = 34.dp, end = 34.dp)
                    .size(16.dp)
                    .rotate(30f)
                    .align(Alignment.BottomEnd)
            )

            Image(
                painter = painterResource(id = R.drawable.arrow_bend_right_up),
                contentDescription = null,
                modifier = Modifier
                    .padding(bottom = 6.dp, end = 6.dp)
                    .size(16.dp)
                    .rotate(30f)
                    .align(Alignment.BottomEnd)
            )

            icons.lastOrNull()?.also {
                Image(
                    painter = it,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(bottom = 20.dp)
                        .size(assetSize)
                        .clip(CircleShape)
                        .align(Alignment.BottomEnd)
                        .border(2.dp, Color.Black, CircleShape)
                )
            }

            icons.firstOrNull()?.also {
                Image(
                    painter = it,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .size(assetSize)
                        .clip(CircleShape)
                        .align(Alignment.BottomEnd)
                        .border(2.dp, Color.Black, CircleShape)
                )
            }
        } else {
            icons.reversed().forEachIndexed { index, painter ->
                val reversedIndex = icons.size - 1 - index
                 val endPadding = ((18 / (1 + (0.2 * (max(reversedIndex - 1, 0))))) * reversedIndex)

                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(bottom = 16.dp, end = (16 + endPadding).dp)
                        .size(assetSize)
                        .clip(CircleShape)
                        .align(Alignment.BottomEnd)
                        .border(2.dp, Color.Black, CircleShape)
                )
            }
        }
    }
}


@Preview
@Composable
fun TransactionStatusIconPreview() {
    GreenThemePreview {
        Column {

            TransactionStatusIcon(
                transactionStatus = Completed,
                icons = listOf(painterResource(id = R.drawable.bitcoin))
            )
            TransactionStatusIcon(
                transactionStatus = Completed,
                icons = listOf(painterResource(id = R.drawable.bitcoin), painterResource(id = R.drawable.liquid)),
                isSwap = true
            )
            TransactionStatusIcon(
                transactionStatus = Unconfirmed(),
                icons = listOf(painterResource(id = R.drawable.bitcoin))
            )
            TransactionStatusIcon(
                transactionStatus = Failed(),
                icons = listOf(painterResource(id = R.drawable.bitcoin))
            )
            TransactionStatusIcon(
                transactionStatus = Failed(),
                icons = listOf(
                    painterResource(id = R.drawable.bitcoin),
                    painterResource(id = R.drawable.liquid),
                )
            )
            TransactionStatusIcon(
                transactionStatus = Failed(),
                icons = listOf(
                    painterResource(id = R.drawable.bitcoin),
                    painterResource(id = R.drawable.liquid),
                    painterResource(id = R.drawable.bitcoin_lightning),
                    painterResource(id = R.drawable.bitcoin_testnet),
                    painterResource(id = R.drawable.liquid_testnet),
                    painterResource(id = R.drawable.bitcoin_lightning_testnet)
                )
            )
        }
    }
}