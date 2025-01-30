package com.blockstream.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.arrow_line_down
import blockstream_green.common.generated.resources.arrow_line_up
import blockstream_green.common.generated.resources.arrow_u_left_down
import blockstream_green.common.generated.resources.arrows_down_up
import blockstream_green.common.generated.resources.id_12_confirmations
import blockstream_green.common.generated.resources.id_d6_confirmations
import blockstream_green.common.generated.resources.id_refundable
import blockstream_green.common.generated.resources.id_unconfirmed
import blockstream_green.common.generated.resources.question
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.gdk.data.Transaction
import com.blockstream.common.looks.transaction.Confirmed
import com.blockstream.common.looks.transaction.TransactionLook
import com.blockstream.common.utils.formatAuto
import com.blockstream.ui.components.GreenRow
import com.blockstream.compose.extensions.directionColor
import com.blockstream.compose.extensions.icon
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.labelMedium
import com.blockstream.compose.theme.md_theme_inverseSurface
import com.blockstream.compose.theme.orange
import com.blockstream.compose.theme.red
import com.blockstream.compose.theme.whiteMedium
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun GreenTransaction(
    modifier: Modifier = Modifier,
    transactionLook: TransactionLook,
    showAccount: Boolean = true,
    onClick: (TransactionLook) -> Unit
) {
    Card(
        onClick = {
            onClick(transactionLook)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp)
            .then(modifier)
    ) {
        Box {

            val status = transactionLook.status
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {

                when (transactionLook.transaction.txType) {
                    Transaction.Type.IN -> Res.drawable.arrow_line_down
                    Transaction.Type.OUT -> Res.drawable.arrow_line_up
                    Transaction.Type.REDEPOSIT -> Res.drawable.arrow_u_left_down
                    Transaction.Type.MIXED -> Res.drawable.arrows_down_up
                    Transaction.Type.UNKNOWN -> Res.drawable.question
                }.let {
                    painterResource(it)
                }.also {
                    Image(
                        painter = it,
                        contentDescription = null
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        GreenRow(
                            padding = 0,
                            space = 2,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(transactionLook.directionText),
                                style = labelLarge,
                                lineHeight = 1.sp
                            )

                            if(!transactionLook.transaction.spv.disabledOrUnconfirmedOrVerified()){
                                Image(
                                    painter = painterResource(transactionLook.transaction.spv.icon()),
                                    contentDescription = "SPV",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            transactionLook.assets.forEachIndexed { index, asset ->
                                Text(
                                    text = asset,
                                    style = labelLarge,
                                    textAlign = TextAlign.End,
                                    color = transactionLook.directionColor(index)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        AnimatedVisibility(visible = status.onProgress) {

                            val isRefundableSwap = transactionLook.transaction.isRefundableSwap
                            if (status.onProgress || isRefundableSwap) {
                                val text = if (isRefundableSwap) {
                                    stringResource(Res.string.id_refundable)
                                } else if (status is Confirmed) {
                                    stringResource(
                                        if (status.confirmationsRequired > 2) Res.string.id_d6_confirmations else Res.string.id_12_confirmations,
                                        status.confirmations
                                    )
                                } else {
                                    stringResource(Res.string.id_unconfirmed)
                                }

                                val bgColor = when {
                                    isRefundableSwap -> red
                                    status is Confirmed -> md_theme_inverseSurface
                                    else -> orange
                                }

                                Text(
                                    text = text, style = bodyMedium, modifier = Modifier
                                        .clip(RoundedCornerShape(100))
                                        .background(bgColor)
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                )

                            }

                        }

                        AnimatedVisibility(visible = !status.onProgress) {
                            // Date
                            Text(
                                text = transactionLook.transaction.createdAtInstant?.formatAuto() ?: "",
                                style = labelMedium,
                                color = whiteMedium
                            )
                        }

                        // Account
                        if (showAccount) {
                            Text(
                                text = transactionLook.transaction.account.name,
                                style = bodyMedium,
                                color = whiteMedium
                            )
                        }
                    }

                    if (transactionLook.transaction.memo.isNotBlank()) {
                        // Memo
                        Text(
                            text = transactionLook.transaction.memo,
                            style = bodyMedium,
                            color = whiteMedium
                        )
                    }

                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = status.onProgress,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                if (status is Confirmed) {
                    LinearProgressIndicator(progress = {
                        status.confirmations / status.confirmationsRequired.toFloat()
                    }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
